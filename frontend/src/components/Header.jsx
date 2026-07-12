export default function Header() {
  return (
    <header className="flex items-center gap-3 border-b border-slate-800 px-6 py-4">
      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-teal-500/20 font-bold text-teal-300">
        IGT
      </div>
      <div>
        <h1 className="text-lg font-semibold leading-tight">MedGemma Assistant</h1>
        <p className="text-xs text-slate-400">
          Image-Guided Therapy · fluoroscopy denoising + AI analysis
        </p>
      </div>
    </header>
  );
}
