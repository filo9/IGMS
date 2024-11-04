import numpy as np
import pyaudio
from scipy.fft import fft


def detect_frequency(chunk, sample_rate):
    """检测主频率"""
    spectrum = np.abs(fft(chunk))[:len(chunk) // 2]
    freqs = np.fft.fftfreq(len(spectrum), 1 / sample_rate)[:len(chunk) // 2]
    return freqs[np.argmax(spectrum)]


def record_audio(sample_rate=80000, duration_per_bit=0.1):
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paInt16, channels=1, rate=sample_rate, input=True, frames_per_buffer=1024)
    frames = []

    print("按回车键开始录音...")
    input()  # 等待用户按下回车开始录音
    print("开始录音，按回车键停止录音")

    while True:
        data = stream.read(1024)
        frames.append(data)

        # 检查是否按下回车键停止录音
        if input() == "":
            print("录音结束")
            break

    stream.stop_stream()
    stream.close()
    p.terminate()

    audio_data = np.frombuffer(b''.join(frames), dtype=np.int16)
    return audio_data, sample_rate


def decode_audio(audio_data, sample_rate, freq0=500, freq1=10000, duration=0.1):
    chunk_size = int(sample_rate * duration)
    binary = ''

    for i in range(0, len(audio_data), chunk_size):
        chunk = audio_data[i:i + chunk_size]
        if len(chunk) == 0:
            continue

        # 计算主频率
        spectrum = np.abs(fft(chunk))[:chunk_size // 2]
        freqs = np.fft.fftfreq(len(spectrum), 1 / sample_rate)[:chunk_size // 2]
        dominant_freq = freqs[np.argmax(spectrum)]

        # 判断频率，确定二进制位
        if abs(dominant_freq - freq0) < abs(dominant_freq - freq1):
            binary += '0'
        else:
            binary += '1'

    # 二进制数据解码为文本
    text = ""
    for i in range(0, len(binary), 8):
        byte = binary[i:i + 8]
        if len(byte) == 8:
            try:
                char = chr(int(byte, 2))
                if 32 <= ord(char) <= 126:  # ASCII 可打印字符范围
                    text += char
            except ValueError:
                continue

    return text


# 主程序：按回车键控制录音并解码
audio_data, sample_rate = record_audio()
decoded_text = decode_audio(audio_data, sample_rate)
print("接收到的文本:", decoded_text)

# 将解码后的数据保存到 wifi.txt 文件中
with open("wifi.txt", "w", encoding="utf-8") as file:
    file.write(decoded_text)
