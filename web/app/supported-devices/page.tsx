import { featureMatrix } from '@/lib/portal/sample-data';

export default function SupportedDevicesPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Supported devices and limitations</h1>
      <p className="mt-3 text-slate-400">
        Current physical evidence is strongest on OPPO CPH2339 Android 12. Other device classes need testing before broader claims.
      </p>
      <div className="mt-8 overflow-hidden rounded-lg border border-slate-800">
        <table className="w-full text-left text-sm">
          <tbody className="divide-y divide-slate-800">
            {featureMatrix.map(([feature, status, requirement]) => (
              <tr key={feature}>
                <td className="px-4 py-3 text-slate-100">{feature}</td>
                <td className="px-4 py-3 text-slate-300">{status}</td>
                <td className="px-4 py-3 text-slate-400">{requirement}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
