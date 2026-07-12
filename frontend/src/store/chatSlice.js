import { createSlice, nanoid } from '@reduxjs/toolkit';

const initialState = {
  connected: false,
  processing: false,
  messages: [],
};

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    setConnected(state, action) {
      state.connected = action.payload;
    },
    setProcessing(state, action) {
      state.processing = action.payload;
    },
    addMessage: {
      reducer(state, action) {
        state.messages.push(action.payload);
      },
      prepare(message) {
        return { payload: { id: nanoid(), ts: Date.now(), ...message } };
      },
    },
    clearChat(state) {
      state.messages = [];
      state.processing = false;
    },
  },
});

export const { setConnected, setProcessing, addMessage, clearChat } = chatSlice.actions;
export default chatSlice.reducer;
