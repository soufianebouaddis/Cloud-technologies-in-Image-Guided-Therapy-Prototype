import { useSelector } from 'react-redux';

export default function StatusBar() {
  const connected = useSelector((s) => s.chat.connected);
  const processing = useSelector((s) => s.chat.processing);

  return (
    <div className="flex items-center gap-4 border-b border-slate-800 bg-slate-900/50 px-6 py-2 text-xs">
      <span className="flex items-center gap-2">
        <span className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-400' : 'bg-rose-500'}`} />
        {connected ? 'Gateway connected' : 'Gateway offline'}
      </span>
      {processing && <span className="text-teal-300">Analyzing scan…</span>}
    </div>
  );
}
