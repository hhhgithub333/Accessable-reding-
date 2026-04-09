import os
import sys
import argparse
import json
import struct
import urllib.parse
import websocket

VIBEVOICE_PORT = 3000
VIBEVOICE_WS_URL = f"ws://127.0.0.1:{VIBEVOICE_PORT}/stream"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", type=str, required=True)
    parser.add_argument("--voice", type=str, default="en-Mike_man")
    parser.add_argument("--output", type=str, required=True)
    args = parser.parse_args()

    try:
        encoded_text = urllib.parse.quote(args.text)
        ws_url = f"{VIBEVOICE_WS_URL}?text={encoded_text}&voice={args.voice}&cfg=1.5&steps=10"

        audio_data = bytearray()
        ws = websocket.create_connection(ws_url, timeout=60)

        while True:
            try:
                message = ws.recv()
                if isinstance(message, bytes):
                    audio_data.extend(message)
                else:
                    continue
            except websocket.WebSocketTimeoutException:
                break
            except Exception:
                break

        ws.close()

        if len(audio_data) == 0:
            raise RuntimeError("未接收到音频数据")

        # 添加 WAV 头
        sample_rate = 24000
        channels = 1
        bits_per_sample = 16
        bytes_per_sample = bits_per_sample // 8

        wav_header = struct.pack('<4sI4s', b'RIFF', 0, b'WAVE')
        wav_header += struct.pack('<4sI', b'fmt ', 16)
        wav_header += struct.pack('<HHIIHH', 1, channels, sample_rate,
                                  sample_rate * channels * bytes_per_sample,
                                  channels * bytes_per_sample, bits_per_sample)
        wav_header += struct.pack('<4sI', b'data', len(audio_data))

        with open(args.output, "wb") as f:
            f.write(wav_header)
            f.write(audio_data)

        result = {"success": True, "output": args.output}
        print(json.dumps(result))

    except Exception as e:
        result = {"success": False, "error": str(e)}
        print(json.dumps(result))


if __name__ == "__main__":
    main()
