import { Client } from '@stomp/stompjs';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
const WS_URL = `${BACKEND_URL.replace(/^http/, 'ws')}/ws/devices`;

/**
 * Opens a STOMP connection to /ws/devices (3.3.10) and subscribes to one
 * /topic/devices/{id}/status per device id, so a device's PENDING -> SUCCESS/FAILED
 * transition reaches the UI the instant the real device (or the timeout job) resolves it,
 * instead of waiting for the next poll.
 */
export function subscribeDeviceStatus(deviceIds, onStatus) {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    onConnect: () => {
      deviceIds.forEach(id => {
        client.subscribe(`/topic/devices/${id}/status`, (message) => {
          try {
            onStatus(JSON.parse(message.body));
          } catch (err) {
            console.error('Failed to parse device status message:', err);
          }
        });
      });
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers?.message, frame.body);
    },
  });
  client.activate();
  return () => client.deactivate();
}
