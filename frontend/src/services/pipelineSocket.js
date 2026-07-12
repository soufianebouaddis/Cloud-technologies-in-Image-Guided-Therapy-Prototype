import { useEffect, useRef, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { setConnected, setProcessing, addMessage } from '../store/chatSlice';

const WS_URL = import.meta.env.VITE_GATEWAY_WS || 'ws://localhost:8085/ws/stream';

/**
 * Maintains the WebSocket to the simulator gateway and maps pipeline
 * events into the Redux chat store. Returns a sendDicom(ArrayBuffer) fn.
 */
export function useGatewaySocket() {
  const dispatch = useDispatch();
  const wsRef = useRef(null);

  useEffect(() => {
    let closed = false;
    let reconnectTimer;

    const connect = () => {
      const ws = new WebSocket(WS_URL);
      ws.binaryType = 'arraybuffer';
      wsRef.current = ws;

      ws.onopen = () => dispatch(setConnected(true));

      ws.onclose = () => {
        dispatch(setConnected(false));
        if (!closed) reconnectTimer = setTimeout(connect, 2000);
      };

      ws.onerror = () => ws.close();

      ws.onmessage = (event) => {
        let data;
        try {
          data = JSON.parse(event.data);
        } catch {
          return;
        }

        switch (data.type) {
          case 'denoised':
            dispatch(
              addMessage({
                role: 'assistant',
                kind: 'image',
                text: 'Denoised fluoroscopy frame',
                image: data.image,
                bytes: data.bytes,
              })
            );
            break;
          case 'finding':
            dispatch(setProcessing(false));
            dispatch(
              addMessage({
                role: 'assistant',
                kind: 'finding',
                finding: data.finding,
                confidence: data.confidence,
                inferenceMs: data.inferenceMs,
                frameId: data.frameId,
              })
            );
            break;
          default:
            break; // status / accepted acks
        }
      };
    };

    connect();

    return () => {
      closed = true;
      clearTimeout(reconnectTimer);
      wsRef.current?.close();
    };
  }, [dispatch]);

  const sendDicom = useCallback((arrayBuffer) => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(arrayBuffer);
      return true;
    }
    return false;
  }, []);

  return { sendDicom };
}
