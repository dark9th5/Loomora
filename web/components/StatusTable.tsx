type Row = {
  label: string;
  value: string;
  note: string;
};

export function StatusTable({ rows }: { rows: Row[] }) {
  return (
    <div className="overflow-hidden rounded-lg border border-slate-800">
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-900 text-slate-300">
          <tr>
            <th className="px-4 py-3 font-medium">Item</th>
            <th className="px-4 py-3 font-medium">Status</th>
            <th className="px-4 py-3 font-medium">Notes</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {rows.map((row) => (
            <tr key={row.label} className="bg-slate-950/60">
              <td className="px-4 py-3 text-slate-100">{row.label}</td>
              <td className="px-4 py-3 text-slate-300">{row.value}</td>
              <td className="px-4 py-3 text-slate-400">{row.note}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
