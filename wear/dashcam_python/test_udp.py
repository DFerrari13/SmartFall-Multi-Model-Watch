import socket
import json
import time

UDP_IP = "127.0.0.1"
UDP_PORT = 5005

print(f"Sending fake FALL_DETECTED signal to {UDP_IP}:{UDP_PORT}...")

# Create the same payload Android would send
current_time_ms = int(time.time() * 1000)
payload = {
    "event": "FALL_DETECTED",
    "event_timestamp": current_time_ms,
    "send_timestamp": current_time_ms
}

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
message = json.dumps(payload).encode('utf-8')
sock.sendto(message, (UDP_IP, UDP_PORT))

print("Signal sent! Check the dashcam terminal to see if it was received.")
