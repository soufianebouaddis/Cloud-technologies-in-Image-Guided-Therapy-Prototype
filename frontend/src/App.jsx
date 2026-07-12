import { useGatewaySocket } from './services/pipelineSocket';
import Header from './components/Header';
import StatusBar from './components/StatusBar';
import ChatWindow from './components/ChatWindow';
import Composer from './components/Composer';

export default function App() {
  const { sendDicom } = useGatewaySocket();

  return (
    <div className="flex h-full flex-col bg-slate-950 text-slate-100">
      <Header />
      <StatusBar />
      <ChatWindow />
      <Composer sendDicom={sendDicom} />
    </div>
  );
}
