import cv2
import socket
import threading
import collections
import time
import json
import os
from datetime import datetime

# Configuration
UDP_IP = "0.0.0.0"  # Listen on all available interfaces
UDP_PORT = 5005
BUFFER_SECONDS = 10  # Keep a larger buffer (e.g. 10s) to account for severe network delays
PRE_FALL_SECONDS = 5 # How many seconds before the exact fall moment to save
POST_FALL_SECONDS = 3 # How many seconds after the exact fall moment to save
FPS = 30             # Approximate FPS for the buffer calculation
BUFFER_SIZE = BUFFER_SECONDS * FPS

# Global state
fall_events_queue = collections.deque()
frames_buffer = collections.deque(maxlen=BUFFER_SIZE)
running = True

def current_time_ms():
    return int(time.time() * 1000)

def udp_listener():
    """Background thread to listen for UDP packets."""
    global running
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((UDP_IP, UDP_PORT))
    sock.settimeout(1.0)
    
    print(f"[*] Listening for UDP fall signals on {UDP_IP}:{UDP_PORT}...")
    
    while running:
        try:
            data, addr = sock.recvfrom(1024)
            receive_timestamp = current_time_ms()
            message = data.decode('utf-8')
            
            try:
                payload = json.loads(message)
                if payload.get("event") == "FALL_DETECTED":
                    event_timestamp = payload.get("event_timestamp")
                    send_timestamp = payload.get("send_timestamp")
                    
                    if event_timestamp and send_timestamp:
                        # Calculate the clock offset between the Android phone and this PC
                        # Assuming the network delay is negligible (e.g., < 10ms on local WiFi)
                        pc_offset = receive_timestamp - send_timestamp
                        
                        # Map the Android fall event timestamp to the PC's local timeline
                        fall_time_pc_ms = event_timestamp + pc_offset
                        
                        print(f"\n[!] FALL DETECTED SIGNAL RECEIVED from {addr}")
                        print(f"    - Android Event Time: {event_timestamp}")
                        print(f"    - Mapped PC Time:     {fall_time_pc_ms}")
                        
                        fall_events_queue.append(fall_time_pc_ms)
                    else:
                        print(f"[!] Received malformed FALL_DETECTED packet: {message}")
            except json.JSONDecodeError:
                print(f"[~] Received non-JSON UDP data: {message}")
        except socket.timeout:
            continue
        except Exception as e:
            if running:
                print(f"[!] UDP Error: {e}")

def main():
    global running
    
    # Start UDP listener thread
    udp_thread = threading.Thread(target=udp_listener, daemon=True)
    udp_thread.start()

    # Initialize Webcam
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("[!] Error: Could not open webcam.")
        running = False
        return

    actual_fps = cap.get(cv2.CAP_PROP_FPS)
    if actual_fps == 0 or actual_fps != actual_fps:
        actual_fps = 30.0
        
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    print(f"[*] Camera initialized. Resolution: {width}x{height} @ {actual_fps} FPS")
    print(f"[*] Dashcam is running. Buffer holds {BUFFER_SECONDS} seconds to handle network delays.")
    print("[*] Press 'q' to quit.")

    if not os.path.exists("fall_videos"):
        os.makedirs("fall_videos")

    while running:
        ret, frame = cap.read()
        if not ret:
            print("[!] Failed to grab frame.")
            break

        frame_timestamp = current_time_ms()
        # Add tuple (timestamp, frame) to buffer
        frames_buffer.append((frame_timestamp, frame))

        cv2.imshow('SmartFall Dashcam', frame)
        
        if cv2.waitKey(1) & 0xFF == ord('q'):
            print("[*] Shutting down...")
            running = False
            break

        # Check if we have pending fall events to process
        if fall_events_queue:
            # Check the oldest event
            fall_time_pc_ms = fall_events_queue[0]
            target_end_time = fall_time_pc_ms + (POST_FALL_SECONDS * 1000)
            
            # We wait until the current time has surpassed the required post-fall timeframe
            if current_time_ms() >= target_end_time:
                fall_events_queue.popleft() # Remove it from queue
                print(f"[*] Extracting video for fall event at mapped time {fall_time_pc_ms}...")
                
                target_start_time = fall_time_pc_ms - (PRE_FALL_SECONDS * 1000)
                
                # Extract frames that fall within the exact mapped time window
                extracted_frames = []
                for f_time, f in list(frames_buffer):
                    if target_start_time <= f_time <= target_end_time:
                        extracted_frames.append(f)
                
                if extracted_frames:
                    timestamp_str = datetime.fromtimestamp(fall_time_pc_ms / 1000.0).strftime("%Y%m%d_%H%M%S")
                    filename = f"fall_videos/fall_event_{timestamp_str}.mp4"
                    
                    fourcc = cv2.VideoWriter_fourcc(*'mp4v') 
                    out = cv2.VideoWriter(filename, fourcc, actual_fps, (width, height))
                    
                    print(f"[*] Saving {len(extracted_frames)} perfectly synchronized frames to {filename}...")
                    for f in extracted_frames:
                        out.write(f)
                    out.release()
                    print(f"[+] Successfully saved fall video: {filename}")
                else:
                    print("[!] Warning: Could not find matching frames for the fall event. Was the network delay too large?")

    # Cleanup
    cap.release()
    cv2.destroyAllWindows()
    udp_thread.join(timeout=2.0)

if __name__ == "__main__":
    main()
