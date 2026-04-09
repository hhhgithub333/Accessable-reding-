import os
import sys
import argparse
import json
import soundfile as sf

VOXCPM_PATH = r"D:\Python\Project\VoxCPM\models\openbmb__VoxCPM1.5"
REFERENCE_AUDIO_PATH = r"D:\Python\Project\tts_test_output\vibevoice_output.wav"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", type=str, required=True)
    parser.add_argument("--output", type=str, required=True)
    args = parser.parse_args()

    try:
        from voxcpm import VoxCPM

        model = VoxCPM.from_pretrained(
            VOXCPM_PATH,
            local_files_only=True
        )

        wav = model.generate(
            text=args.text,
            prompt_wav_path=REFERENCE_AUDIO_PATH,
            prompt_text="Hello, this is a speech synthesis test from VibeVoice.",
            cfg_value=2.0,
            inference_timesteps=6,
            normalize=False,
            denoise=False,
            retry_badcase=True,
            retry_badcase_max_times=3,
            retry_badcase_ratio_threshold=6.0,
        )

        sf.write(args.output, wav, model.tts_model.sample_rate)

        result = {"success": True, "output": args.output}
        print(json.dumps(result))

    except Exception as e:
        result = {"success": False, "error": str(e)}
        print(json.dumps(result))


if __name__ == "__main__":
    main()