import os
import sys
import argparse
import json

XTTS_V2_PATH = r"D:\Python\Project\xtts_v2"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", type=str, required=True)
    parser.add_argument("--voice_file", type=str, required=True)
    parser.add_argument("--output", type=str, required=True)
    args = parser.parse_args()

    try:
        from TTS.api import TTS

        tts = TTS(
            model_path=XTTS_V2_PATH,
            config_path=os.path.join(XTTS_V2_PATH, "config.json"),
            gpu=True
        )

        tts.tts_to_file(
            text=args.text,
            file_path=args.output,
            speaker_wav=args.voice_file,
            language="zh"
        )

        result = {"success": True, "output": args.output}
        print(json.dumps(result))

    except Exception as e:
        result = {"success": False, "error": str(e)}
        print(json.dumps(result))


if __name__ == "__main__":
    main()