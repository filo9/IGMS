import numpy as np
import wave
from scipy.fft import fft
import struct

# OFDM 参数
SAMPLE_RATE = 44100       # 采样率
NUM_SUBCARRIERS = 3       # 子载波数量
SYMBOL_DURATION = 0.3     # 每个符号的持续时间
subcarrier_frequencies = np.linspace(1000, 5000, NUM_SUBCARRIERS)  # 子载波频率

# 将二进制字符串转换为文本
def binary_to_string(binary_data):
    chars = [chr(int(binary_data[i:i+8], 2)) for i in range(0, len(binary_data), 8)]
    return ''.join(chars)

# 读取音频文件并获取数据
def read_audio_file(filename):
    with wave.open(filename, 'rb') as wf:
        num_frames = wf.getnframes()
        audio_data = struct.unpack('<' + 'h' * num_frames, wf.readframes(num_frames))
    return np.array(audio_data)

# 解码 OFDM 信号
def ofdm_demodulate(ofdm_signal):
    num_samples_per_symbol = int(SYMBOL_DURATION * SAMPLE_RATE)
    num_symbols = len(ofdm_signal) // num_samples_per_symbol
    binary_data = ''

    for i in range(num_symbols):
        # 获取当前符号数据
        symbol_data = ofdm_signal[i * num_samples_per_symbol: (i + 1) * num_samples_per_symbol]
        
        # 进行 FFT 以提取频率分量
        fft_result = np.abs(fft(symbol_data)[:num_samples_per_symbol // 2])
        frequencies = np.fft.fftfreq(num_samples_per_symbol, 1 / SAMPLE_RATE)[:num_samples_per_symbol // 2]

        # 检测子载波频率对应的频率分量
        bits = ''
        for freq in subcarrier_frequencies:
            # 找到最接近的频率索引
            freq_index = np.argmin(np.abs(frequencies - freq))
            # 根据 FFT 结果检测频率峰值
            bits += '1' if fft_result[freq_index] > np.max(fft_result) * 0.5 else '0'
        
        binary_data += bits

    return binary_data

# 主程序
if __name__ == "__main__":
    # 读取音频文件
    filename = "ofdm_signal.wav"
    ofdm_signal = read_audio_file(filename)
    
    # 解调 OFDM 信号
    print("正在解码 OFDM 信号...")
    binary_data = ofdm_demodulate(ofdm_signal)
    
    # 将二进制数据转换为文本
    decoded_message = binary_to_string(binary_data)
    print("解码完成，解码内容为：")
    print(decoded_message)