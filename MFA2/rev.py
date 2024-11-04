import numpy as np
import pyaudio
import wave
from scipy.fft import fft
import time

# 参数配置
SAMPLE_RATE = 48000  # 更改为常用的采样率，兼容性更好
DURATION_PER_BIT = 0.01  # 每个比特的时长（秒）
FREQ0 = 500  # 表示“0”的频率
FREQ1 = 10000  # 表示“1”的频率
OUTPUT_WAV_FILE = "audio_signal.wav"  # 保存录音的文件名


# 检测频率
def detect_frequency(chunk, sample_rate):
    spectrum = np.abs(fft(chunk))[:len(chunk) // 2]
    freqs = np.fft.fftfreq(len(spectrum), 1 / sample_rate)[:len(chunk) // 2]
    return freqs[np.argmax(spectrum)]


# 录音函数
def record_audio(sample_rate=SAMPLE_RATE, record_seconds=10, buffer_size=4096):
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paInt16, channels=1, rate=sample_rate, input=True, frames_per_buffer=buffer_size)

    print("开始录音...")
    frames = []

    with wave.open(OUTPUT_WAV_FILE, 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(p.get_sample_size(pyaudio.paInt16))
        wf.setframerate(sample_rate)

        start_time = time.time()
        while time.time() - start_time < record_seconds:
            try:
                data = stream.read(buffer_size, exception_on_overflow=False)
                wf.writeframes(data)  # 实时写入文件
                frames.append(data)  # 同时保存数据到内存
            except OSError as e:
                print(f"警告: {e}")
                continue

    print("录音结束")
    stream.stop_stream()
    stream.close()
    p.terminate()

    print(f"音频已保存到 {OUTPUT_WAV_FILE}")
    audio_data = np.frombuffer(b''.join(frames), dtype=np.int16)
    return audio_data, sample_rate


# 从 .wav 文件读取音频数据
def read_wav_file(file_path):
    with wave.open(file_path, 'rb') as wf:
        sample_rate = wf.getframerate()
        frames = wf.readframes(wf.getnframes())
        audio_data = np.frombuffer(frames, dtype=np.int16)
    return audio_data, sample_rate


# 解码音频信号
def decode_audio(audio_data, sample_rate, freq0=FREQ0, freq1=FREQ1, duration=DURATION_PER_BIT):
    chunk_size = int(sample_rate * duration)
    binary = ''

    for i in range(0, len(audio_data), chunk_size):
        chunk = audio_data[i:i + chunk_size]
        if len(chunk) == 0:
            continue

        dominant_freq = detect_frequency(chunk, sample_rate)

        # 根据频率判断0或1
        if abs(dominant_freq - freq0) < abs(dominant_freq - freq1):
            binary += '0'
        else:
            binary += '1'

    # 二进制数据转换为文本
    text = ""
    for i in range(0, len(binary), 8):
        byte = binary[i:i + 8]
        if len(byte) == 8:
            try:
                char = chr(int(byte, 2))
                if 32 <= ord(char) <= 126:
                    text += char
            except ValueError:
                continue

    return text


# 主程序
def main():
    choice = input("选择输入方式：1. 录音  2. 从文件读取 (输入1或2): ")

    if choice == "1":
        audio_data, sample_rate = record_audio()
    elif choice == "2":
        file_path = OUTPUT_WAV_FILE  # 默认读取录制的 .wav 文件
        audio_data, sample_rate = read_wav_file(file_path)
    else:
        print("无效选择")
        return

    decoded_text = decode_audio(audio_data, sample_rate)
    print("接收到的文本:", decoded_text)

    # 将解码后的文本保存到 wifi.txt 文件中
    with open("wifi.txt", "w", encoding="utf-8") as file:
        file.write(decoded_text)
    print("解码结果已保存到 wifi.txt")


if __name__ == "__main__":
    main()