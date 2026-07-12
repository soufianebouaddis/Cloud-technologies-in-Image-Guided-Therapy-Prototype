export default function MessageBubble({ m }) {
  const isUser = m.role === 'user';

  if (m.role === 'system') {
    return <div className="text-center text-xs text-amber-300/80">{m.text}</div>;
  }

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[78%] rounded-2xl px-4 py-3 ${
          isUser ? 'bg-teal-600 text-white' : 'bg-slate-800 text-slate-100'
        }`}
      >
        {!isUser && <div className="mb-1 text-xs font-medium text-teal-300">MedGemma</div>}

        {m.kind === 'text' && <p className="whitespace-pre-wrap text-sm">{m.text}</p>}

        {m.kind === 'image' && (
          <div>
            <p className="mb-2 text-xs text-slate-300">
              {m.text} · {(m.bytes / 1024 / 1024).toFixed(1)} MB raw
            </p>
            <img
              src={m.image}
              alt="denoised fluoroscopy frame"
              className="max-h-80 rounded-lg border border-slate-700"
            />
          </div>
        )}

        {m.kind === 'finding' && (
          <div>
            <p className="whitespace-pre-wrap text-sm">{m.finding}</p>
            <div className="mt-2 flex flex-wrap gap-2 text-xs">
              <span className="rounded bg-slate-700 px-2 py-0.5">
                confidence {(m.confidence ?? 0).toFixed(2)}
              </span>
              <span className="rounded bg-slate-700 px-2 py-0.5">{m.inferenceMs} ms</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
