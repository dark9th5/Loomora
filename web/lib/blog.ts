import matter from 'gray-matter';

export interface BlogPost {
  slug: string;
  title: string;
  excerpt: string;
  content: string;
  date: string;
  category: string;
  tags: string[];
  readTime: string;
  author: string;
}

const SAMPLE_POSTS: BlogPost[] = [
  {
    slug: 'local-first-voice-recording-privacy',
    title: 'Why Local-First Voice Recording Matters for Your Privacy',
    excerpt: 'Discover how keeping your voice notes and meeting audio on-device protects your sensitive conversations from unauthorized cloud exposure.',
    content: `
Voice recordings contain intimate, high-stakes information—strategic business decisions, medical notes, or personal reflections. In an era where cloud services routinely ingest user data for AI training, **local-first architecture** is no longer optional.

### What is Local-First Audio?
Local-first means that your device is the primary source of truth. Audio files recorded in Loomora are stored directly in your Android internal storage. 

Key advantages include:
- **Zero Internet Required:** Record lectures, interviews, and voice memos anywhere.
- **No Login Wall:** You never need to create an account or verify an email just to record and play audio.
- **Complete File Ownership:** You can delete, export, or transfer your audio files at any time.

### Explicit Disclosure Before Cloud AI
When optional AI transcription or smart insights are requested, Loomora requires explicit user consent before transmitting any data over HTTPS. Your core recordings remain untouched locally.
    `,
    date: '2026-07-25',
    category: 'Privacy',
    tags: ['Privacy', 'Local-First', 'Security', 'Android'],
    readTime: '4 min read',
    author: 'Loomora Engineering Team',
  },
  {
    slug: 'non-destructive-audio-editing-explained',
    title: 'Non-Destructive Audio Editing: Preserve Every Original Recording',
    excerpt: 'Learn how non-destructive editing allows you to trim and enhance recordings while keeping your raw audio files 100% intact.',
    content: `
Traditional voice recording apps overwrite your original audio file when you trim or edit a recording. If you make a mistake or need the omitted segment later, it is gone forever.

### The Non-Destructive Approach
Loomora separates your edit operations (trims, deletes, speech clarity filters) into an immutable \`EditRecipe\`. 

When you export an edited recording:
1. Loomora reads the original raw \`.m4a\` file.
2. It processes the operations into a new output file named \`[Title]_edited.m4a\`.
3. Your original raw recording remains **100% untouched** on storage.

This approach guarantees that you can revert edits, undo actions, or restore original audio whenever necessary.
    `,
    date: '2026-07-24',
    category: 'Engineering',
    tags: ['Audio Editing', 'Android', 'MediaRecorder', 'Productivity'],
    readTime: '5 min read',
    author: 'Loomora Audio Team',
  },
  {
    slug: 'turning-meetings-into-actionable-ai-notes',
    title: 'How to Turn 60-Minute Meetings into 2-Minute Actionable Summaries',
    excerpt: 'Streamline post-meeting workflows with automated speaker labels, key decision tracking, and clear action item extraction.',
    content: `
Sitting through 60-minute meeting recordings to find one specific commitment wastes valuable time. 

### Structured AI Insights
Loomora produces structured output designed for immediate action:
- **Timestamped Transcripts:** Jump directly to specific moments in the recording.
- **Key Points:** Get a 3-bullet executive summary.
- **Decisions Made:** Track strategic agreements explicitly.
- **Action Items:** Extract tasks with optional assignees and due dates.

By combining local storage with optional provider-neutral AI processing, you get modern intelligence with total peace of mind.
    `,
    date: '2026-07-23',
    category: 'Productivity',
    tags: ['AI Notes', 'Productivity', 'Meetings', 'Transcripts'],
    readTime: '3 min read',
    author: 'Loomora Product Team',
  },
];

export function getAllPosts(): BlogPost[] {
  return SAMPLE_POSTS;
}

export function getPostBySlug(slug: string): BlogPost | undefined {
  return SAMPLE_POSTS.find((p) => p.slug === slug);
}

export function getCategories(): string[] {
  return Array.from(new Set(SAMPLE_POSTS.map((p) => p.category)));
}

export function getTags(): string[] {
  return Array.from(new Set(SAMPLE_POSTS.flatMap((p) => p.tags)));
}
