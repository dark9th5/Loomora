#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
import argparse
import datetime
import shutil
import subprocess
import sys

def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")

def write_text(path: Path, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value, encoding="utf-8")

def backup(path: Path, repo: Path, backup_root: Path) -> None:
    if not path.exists():
        return
    dest = backup_root / path.relative_to(repo)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, dest)

def replace_required(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Không tìm thấy mẫu cần sửa: {label}")
    return text.replace(old, new, 1)

def replace_all_if_present(text: str, old: str, new: str) -> str:
    return text.replace(old, new) if old in text else text

def copy_overlay(package_root: Path, repo: Path, backup_root: Path, changed: list[str]) -> None:
    overlay = package_root / "overlay"
    for src in sorted(overlay.rglob("*")):
        if not src.is_file():
            continue
        rel = src.relative_to(overlay)
        dest = repo / rel
        if dest.exists() and dest.read_bytes() == src.read_bytes():
            continue
        backup(dest, repo, backup_root)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        changed.append(str(rel))

def patch_audio(repo: Path, backup_root: Path, changed: list[str]) -> None:
    engine = repo / "core/audio/src/main/java/com/loomora/core/audio/engine/AudioRecordEngine.kt"
    service = repo / "core/audio/src/main/java/com/loomora/core/audio/service/AudioRecorderService.kt"

    if engine.exists():
        text = read_text(engine)
        updated = text
        updated = replace_all_if_present(updated, "setAudioSamplingRate(48_000)", "setAudioSamplingRate(AudioCaptureSpec.SAMPLE_RATE_HZ)")
        updated = replace_all_if_present(updated, "setAudioEncodingBitRate(128_000)", "setAudioEncodingBitRate(AudioCaptureSpec.AAC_BIT_RATE)")
        updated = replace_all_if_present(updated, "setAudioChannels(1)", "setAudioChannels(AudioCaptureSpec.CHANNEL_COUNT)")
        if updated != text:
            backup(engine, repo, backup_root)
            write_text(engine, updated)
            changed.append(str(engine.relative_to(repo)))

    if service.exists():
        text = read_text(service)
        updated = text
        import_line = "import com.loomora.core.audio.engine.AudioCaptureSpec\n"
        if import_line not in updated:
            anchor = "import com.loomora.core.audio.engine.AudioRecordEngine\n"
            if anchor not in updated:
                raise RuntimeError("Không tìm thấy import AudioRecordEngine trong AudioRecorderService")
            updated = updated.replace(anchor, anchor + import_line, 1)
        updated = updated.replace('mimeType = "audio/aac"', "mimeType = AudioCaptureSpec.MIME_TYPE")
        updated = updated.replace("sampleRate = 44100", "sampleRate = AudioCaptureSpec.SAMPLE_RATE_HZ")
        updated = updated.replace("channels = 2", "channels = AudioCaptureSpec.CHANNEL_COUNT")
        updated = updated.replace("bitrate = 128000", "bitrate = AudioCaptureSpec.AAC_BIT_RATE")
        if updated != text:
            backup(service, repo, backup_root)
            write_text(service, updated)
            changed.append(str(service.relative_to(repo)))

def patch_insights(repo: Path, backup_root: Path, changed: list[str]) -> None:
    engine = repo / "core/offlineai/src/main/java/com/loomora/core/offlineai/ExtractiveMeetingInsightEngine.kt"
    if not engine.exists():
        return
    text = read_text(engine)
    updated = text.replace(
        "val actionCandidates = selectDiverse(scored.filter { HeuristicActionItemExtractor.isActionable(it.text) }, 5)",
        "val actionCandidates = selectDiverse(scored.filter { HeuristicActionItemExtractor.isActionable(it.text) }, 8)"
    )
    updated = updated.replace(
        """val actions = actionCandidates.mapNotNull { candidate ->
            HeuristicActionItemExtractor.extract(candidate.text, candidate.segmentIds)
        }.distinctBy { normalizeForComparison(it.task) }""",
        """val actions = actionCandidates.flatMap { candidate ->
            HeuristicActionItemExtractor.extractAll(candidate.text, candidate.segmentIds)
        }.distinctBy {
            listOf(
                normalizeForComparison(it.task),
                normalizeForComparison(it.assignee.orEmpty()),
                it.dueDate?.lowercase().orEmpty()
            ).joinToString("|")
        }"""
    )
    if updated != text:
        backup(engine, repo, backup_root)
        write_text(engine, updated)
        changed.append(str(engine.relative_to(repo)))

def patch_recording_detail(repo: Path, backup_root: Path, changed: list[str]) -> None:
    vm = repo / "feature/recordingdetail/src/main/java/com/loomora/feature/recordingdetail/RecordingDetailViewModel.kt"
    if vm.exists():
        text = read_text(vm)
        updated = text

        if "import com.loomora.core.offlineai.TranscriptionModelSelector\n" not in updated:
            anchor = "import com.loomora.core.offlineai.TranscriptionPerformanceProfile\n"
            if anchor not in updated:
                raise RuntimeError("Không tìm thấy import TranscriptionPerformanceProfile")
            updated = updated.replace(
                anchor,
                anchor + "import com.loomora.core.offlineai.TranscriptionModelSelector\n",
                1
            )

        old_preflight = """if (!offlineModelRepository.hasReadyModels(setOf(ModelCapability.TRANSCRIPTION))) {
                transcriptionModelMissing.value = true
                return@launch
            }"""
        new_preflight = """val selectedTranscriptionModel = TranscriptionModelSelector.select(
                requestedLanguageTag = preferences.transcriptLanguage.tag,
                records = offlineModelRepository.models.first(),
                manuallySelectedId = preferences.transcriptionModelId
            )
            if (selectedTranscriptionModel == null) {
                transcriptionModelMissing.value = true
                return@launch
            }"""
        if old_preflight in updated:
            updated = updated.replace(old_preflight, new_preflight, 1)
        elif "val selectedTranscriptionModel = TranscriptionModelSelector.select(" not in updated:
            raise RuntimeError("Không tìm thấy block kiểm tra transcription model")

        old_model = """transcriptionModelId = preferences.transcriptionModelId
                        ?: DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,"""
        new_model = "transcriptionModelId = selectedTranscriptionModel.manifest.id,"
        if old_model in updated:
            updated = updated.replace(old_model, new_model, 1)
        elif new_model not in updated:
            raise RuntimeError("Không tìm thấy block model mặc định để sửa")

        old_identity = """val identityString = if (sortedEvidenceCsv.isNotBlank()) {
                "$targetRecordingId|$sortedEvidenceCsv"
            } else {
                "$targetRecordingId|${normalizeTaskIdentity(title)}"
            }"""
        new_identity = """val identityString = listOf(
                targetRecordingId,
                sortedEvidenceCsv,
                normalizeTaskIdentity(title),
                normalizeTaskIdentity(assignee.orEmpty())
            ).joinToString("|")"""
        if old_identity in updated:
            updated = updated.replace(old_identity, new_identity, 1)
        elif new_identity not in updated:
            # Support older title-only version.
            older = """val normalizedIdentity = listOf(
                targetRecordingId,
                normalizeTaskIdentity(title)
            ).joinToString("|")"""
            if older in updated:
                updated = updated.replace(older, new_identity.replace("identityString", "normalizedIdentity"), 1)
            else:
                raise RuntimeError("Không tìm thấy block tạo task identity")

        updated = updated.replace(
            "TranscriptSpeakerFusion.compact(transcript?.segments.orEmpty())",
            "TranscriptSpeakerFusion.displayRows(transcript?.segments.orEmpty())"
        )

        # Remove now-unused default catalog import only when no longer referenced.
        if "DefaultOfflineModelCatalog." not in updated:
            updated = updated.replace("import com.loomora.core.offlineai.DefaultOfflineModelCatalog\n", "")

        if updated != text:
            backup(vm, repo, backup_root)
            write_text(vm, updated)
            changed.append(str(vm.relative_to(repo)))

    screen = repo / "feature/recordingdetail/src/main/java/com/loomora/feature/recordingdetail/RecordingDetailScreen.kt"
    if screen.exists():
        text = read_text(screen)
        updated = text.replace("TranscriptSpeakerFusion.compact(", "TranscriptSpeakerFusion.displayRows(")
        if updated != text:
            backup(screen, repo, backup_root)
            write_text(screen, updated)
            changed.append(str(screen.relative_to(repo)))

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=".", help="Thư mục gốc repository Loomora")
    args = parser.parse_args()

    package_root = Path(__file__).resolve().parent.parent
    repo = Path(args.repo).resolve()
    required = [
        repo / "settings.gradle.kts",
        repo / "core/offlineai",
        repo / "core/audio",
        repo / "feature/recordingdetail",
    ]
    missing = [str(p) for p in required if not p.exists()]
    if missing:
        print("Không đúng thư mục gốc Loomora. Thiếu:", *missing, sep="\n- ", file=sys.stderr)
        return 2

    timestamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_root = repo / ".loomora-unified-backup" / timestamp
    changed: list[str] = []

    try:
        copy_overlay(package_root, repo, backup_root, changed)
        patch_audio(repo, backup_root, changed)
        patch_insights(repo, backup_root, changed)
        patch_recording_detail(repo, backup_root, changed)
    except Exception as exc:
        print(f"Áp dụng thất bại: {exc}", file=sys.stderr)
        print(f"Backup (nếu có): {backup_root}", file=sys.stderr)
        return 1

    print("Đã áp dụng Loomora Unified Code Fix.")
    if changed:
        print("File thay đổi:")
        for item in sorted(set(changed)):
            print(f"- {item}")
    else:
        print("Không có thay đổi; bản sửa có thể đã được áp dụng trước đó.")
    print(f"Backup: {backup_root}")

    try:
        commit = subprocess.check_output(
            ["git", "-C", str(repo), "rev-parse", "--short", "HEAD"],
            text=True,
            stderr=subprocess.DEVNULL
        ).strip()
        print(f"HEAD hiện tại: {commit}")
    except Exception:
        pass
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
