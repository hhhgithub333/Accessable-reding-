import os
import sys
import argparse
import json
import torch
import soundfile as sf

# 添加 IndexTTS 路径
INDEX_TTS_PATH = r"D:\Git\uv_work\index-tts"
if INDEX_TTS_PATH not in sys.path:
    sys.path.insert(0, INDEX_TTS_PATH)

# 模型配置路径
MODEL_DIR = os.path.join(INDEX_TTS_PATH, "checkpoints")
CONFIG_PATH = os.path.join(MODEL_DIR, "config.yaml")
DEFAULT_SPK_AUDIO = os.path.join(INDEX_TTS_PATH, "examples", "voice_07.wav")
DEFAULT_EMO_AUDIO = os.path.join(INDEX_TTS_PATH, "examples", "emo_sad.wav")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", type=str, required=True)
    parser.add_argument("--output", type=str, required=True)
    parser.add_argument("--voice_file", type=str, default=DEFAULT_SPK_AUDIO)
    parser.add_argument("--emo_file", type=str, default=DEFAULT_EMO_AUDIO)
    parser.add_argument("--emo_alpha", type=float, default=0.9)
    args = parser.parse_args()

    try:
        from indextts.infer_v2 import IndexTTS2

        # 加载模型（首次会慢，后续会缓存）
        print("加载 IndexTTS2 模型中...", file=sys.stderr)
        tts = IndexTTS2(
            cfg_path=CONFIG_PATH,
            model_dir=MODEL_DIR,
            use_fp16=False,
            use_cuda_kernel=False,
            use_deepspeed=False
        )
        print("模型加载完成", file=sys.stderr)

        # 检查参考音频
        if not os.path.exists(args.voice_file):
            raise FileNotFoundError(f"参考音频不存在: {args.voice_file}")

        # 合成语音
        tts.infer(
            spk_audio_prompt=args.voice_file,
            text=args.text,
            output_path=args.output,
            emo_audio_prompt=args.emo_file if os.path.exists(args.emo_file) else None,
            emo_alpha=args.emo_alpha,
            verbose=False
        )

        # 读取生成的音频
        with open(args.output, "rb") as f:
            audio_data = f.read()

        result = {"success": True, "output": args.output}
        print(json.dumps(result))

    except Exception as e:
        result = {"success": False, "error": str(e)}
        print(json.dumps(result))


if __name__ == "__main__":
    main()