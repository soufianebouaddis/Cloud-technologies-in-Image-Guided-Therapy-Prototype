import { useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addMessage, setProcessing } from '../store/chatSlice';

export default function Composer({ sendDicom }) {
  const dispatch = useDispatch();
  const connected = useSelector((s) => s.chat.connected);
  const inputRef = useRef(null);

  const onFile = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const buffer = await file.arrayBuffer();
    dispatch(
      addMessage({
        role: 'user',
        kind: 'text',
        text: `Uploaded DICOM: ${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`,
      })
    );

    if (sendDicom(buffer)) {
      dispatch(setProcessing(true));
    } else {
      dispatch(
        addMessage({
          role: 'system',
          kind: 'text',
          text: 'Gateway not connected — is the simulator running on :8085?',
        })
      );
    }
  };

  return (
    <footer className="border-t border-slate-800 px-6 py-4">
      <div className="mx-auto flex max-w-3xl items-center gap-3">
        <input
          ref={inputRef}
          type="file"
          accept=".dcm,application/dicom"
          className="hidden"
          onChange={onFile}
        />
        <button
          onClick={() => inputRef.current?.click()}
          disabled={!connected}
          className="flex items-center gap-2 rounded-lg bg-teal-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-teal-500 disabled:cursor-not-allowed disabled:opacity-40"
        >
          ⬆ Upload DICOM (.dcm)
        </button>
        <span className="text-sm text-slate-500">
          Send a fluoroscopy scan to MedGemma for analysis
        </span>
      </div>
    </footer>
  );
}
