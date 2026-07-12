import { useSelector } from 'react-redux';
import { useEffect, useRef } from 'react';
import MessageBubble from './MessageBubble';

export default function ChatWindow() {
  const messages = useSelector((s) => s.chat.messages);
  const processing = useSelector((s) => s.chat.processing);
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, processing]);

  return (
    <main className="flex-1 overflow-y-auto px-6 py-6">
      <div className="mx-auto flex max-w-3xl flex-col gap-4">
        {messages.length === 0 && (
          <div className="mt-20 text-center text-slate-500">
            <p className="text-sm">Upload a DICOM (.dcm) fluoroscopy frame to start.</p>
            <p className="mt-1 text-xs">
              It streams through the simulator → cpp denoising → MedGemma analysis.
            </p>
          </div>
        )}

        {messages.map((m) => (
          <MessageBubble key={m.id} m={m} />
        ))}

        {processing && (
          <div className="flex justify-start">
            <div className="rounded-2xl bg-slate-800 px-4 py-3 text-sm text-slate-400">
              <span className="animate-pulse">MedGemma is analyzing the scan…</span>
            </div>
          </div>
        )}

        <div ref={endRef} />
      </div>
    </main>
  );
}
