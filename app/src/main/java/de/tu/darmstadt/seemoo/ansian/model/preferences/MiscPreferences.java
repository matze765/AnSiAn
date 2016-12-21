package de.tu.darmstadt.seemoo.ansian.model.preferences;

import java.io.File;

import android.os.Environment;
import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation.DemoType;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.sources.HackrfSource;
import de.tu.darmstadt.seemoo.ansian.model.sources.IQSourceInterface.SourceType;

public class MiscPreferences extends MySharedPreferences {
    public MiscPreferences(MainActivity activity) {
        super(activity);
    }

    private static final String LOGTAG = "MiscPreferences";

    private String rtlsdrIP;

    private boolean filesourceRepeat;
    private boolean hackrfAmplify;
    private boolean hackrfAntennaPower;
    private boolean rtlsrdExternalServer;
    private boolean rtlsdrManualGain;
    private boolean recordingStopAfter;
    private boolean autostart;

    private int filesourceFileFormat;
    private int rtlsdrPort;
    private int rtlsdrFrequencyCorrection;
    private int rtlsdrFrequencyShift;
    private int hackrfFrequencyShift;
    private int fftSize;
    private int vgaRxGain;
    private int rtlsdrGain;
    private int rtlsdrIfGain;
    private int sourceSamplerate;
    private int lnaGain;
    private int recordingStopAfterValue;
    private int recordingStoppAfterUnit;
    private int recordingSampleRate;


    private de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation.DemoType demodulation;
    private SourceType sourceType;

    private int fftAveraging;

    private String logfile;

    private boolean logging;

    private boolean showLog;

    private boolean showDebugInformation;

    private long fileSourceFrequency;

    private int fileSourceSampleRate;

    // tranmission
    private Modulation.TxMode send_txMode;
    private String send_payloadText;
    private int send_vgaGain;
    private boolean send_amplifier;
    private boolean send_antennaPower;
    private int send_sampleRate;
    private int send_frequency;
    private String send_filename;

    // morse transmission
    private int morse_wpm;
    private int morse_frequency;

    //rds
    private int rds_audio_source;

    /**
     * Will check if any preference conflicts with the current state of the app
     * and fix it
     */
    public void loadPreference() {

        // source
        sourceType = SourceType.values()[getInt("source_type", 2)];
        sourceSamplerate = getInt("source_samplerate", 2000000);

        // file source
        filesourceFileFormat = getInt("filesource_format", 0);
        filesourceRepeat = getBoolean("filesource_repeat", true);
        fileSourceSampleRate = getInt("filesource_samplerate", 0);
        fileSourceFrequency = getLong("filesource_frequency", 0);

        // Hack RF
        hackrfAmplify = getBoolean("hackrf_amplify", false);
        hackrfAntennaPower = getBoolean("hackrf_antenna_power", false);
        hackrfFrequencyShift = getInt("hackrf_frequency_shift", 0);
        vgaRxGain = getInt("hackrf_vga_rx_gain", HackrfSource.MAX_VGA_RX_GAIN / 2);
        lnaGain = getInt("hackrf_lna_gain", HackrfSource.MAX_LNA_GAIN / 2);

        // RTL
        rtlsdrIP = getString("rtlsdr_ip", "");
        rtlsdrPort = getInt("rtlsdr_port", 1234);
        rtlsrdExternalServer = getBoolean("rtlsdr_external_server", false);
        rtlsdrFrequencyCorrection = getInt("rtlsdr_frequency_correction", 0);
        rtlsdrFrequencyShift = getInt("rtlsdr_frequency_shift", 0);
        rtlsdrManualGain = getBoolean("rtlsdr_manual_gain", false);
        rtlsdrGain = getInt("rtlsdr_gain", 0);
        rtlsdrIfGain = getInt("rtlsdr_if_gain", 0);

        // FFT
        fftAveraging = getInt("fft_averaging", 1);
        fftSize = getInt("fft_size", 1024);

        // Recording
        recordingStopAfter = getBoolean("recording_stop_after", false);
        recordingStopAfterValue = getInt("recording_stop_after_value", 10);
        recordingStoppAfterUnit = getInt("recording_stop_after_unit", 0);

        // misc
        autostart = getBoolean("autostart", false);
        demodulation = DemoType.values()[getInt("demodulation", 0)];
        showDebugInformation = getBoolean("show_debug_information", false);
        logging = getBoolean("logging", false);
        logfile = getString("logfile", "");
        showLog = getBoolean("show_log", false);

        // transmission
        send_txMode = Modulation.TxMode.values()[getInt("send_tx_mode", 0)];
        send_payloadText = getString("send_payload_text", "Hello World");
        send_vgaGain = getInt("send_vga_gain", 40);
        send_amplifier = getBoolean("send_amplifier", false);
        send_antennaPower = getBoolean("send_antenna_power", false);
        send_sampleRate = getInt("send_sample_rate", 1000000);
        send_frequency = getInt("send_frequency", 97000000);
        send_filename = getString("send_file_name", Environment.getExternalStorageDirectory().getAbsolutePath() + "/samples.iq");

        // morse transmission
        morse_wpm = getInt("morse_wpm", 6);
        morse_frequency = getInt("morse_frequency", 1000);

        // rds transmission
        rds_audio_source = getInt("rds_audio_source",0);


    }

    public void savePreference() {
        // create editor
        MyEditor editor = edit();

        // source
        editor.putString("source_type", "" + sourceType.ordinal());
        editor.putInt("source_samplerate", sourceSamplerate);

        // file
        editor.putString("filesource_format", "" + filesourceFileFormat);
        editor.putBoolean("filesource_repeat", filesourceRepeat);
        editor.putInt("filesource_samplerate", fileSourceSampleRate);
        editor.putLong("filesource_frequency", fileSourceFrequency);

        // Hack RF
        editor.putBoolean("hackrf_amplify", hackrfAmplify);
        editor.putBoolean("hackrf_antenna_power", hackrfAntennaPower);
        editor.putInt("hackrf_frequency_shift", hackrfFrequencyShift);
        editor.putInt("hackrf_vga_rx_gain", vgaRxGain);
        editor.putInt("hackrf_lna_gain", lnaGain);

        // RTL
        editor.putString("rtlsdr_ip", rtlsdrIP);
        editor.putInt("rtlsdr_port", rtlsdrPort);
        editor.putBoolean("rtlsdr_external_server", rtlsrdExternalServer);
        editor.putInt("rtlsdr_frequency_correction", rtlsdrFrequencyCorrection);
        editor.putInt("rtlsdr_frequency_shift", rtlsdrFrequencyShift);
        editor.putBoolean("rtlsdr_manual_gain", rtlsdrManualGain);
        editor.putInt("rtlsdr_gain", rtlsdrGain);
        editor.putInt("rtlsdr_if_gain", rtlsdrIfGain);

        // FFT
        editor.putString("fft_size", "" + fftSize);
        editor.putString("fft_averaging", "" + fftAveraging);

        // Recording
        editor.putBoolean("recording_stop_after", recordingStopAfter);
        editor.putInt("recording_stop_after_value", recordingStopAfterValue);
        editor.putInt("recording_stop_after_unit", recordingStoppAfterUnit);

        // misc
        editor.putBoolean("autostart", autostart);
        editor.putInt("demodulation", demodulation.ordinal());
        editor.putBoolean("logging", logging);
        editor.putString("logfile", logfile);
        editor.putBoolean("show_log", showLog);
        editor.putBoolean("show_debug_information", showDebugInformation);

        // transmission
        editor.putInt("send_tx_mode", send_txMode.ordinal());
        editor.putString("send_payload_text", send_payloadText);
        editor.putInt("send_vga_gain", send_vgaGain);
        editor.putBoolean("send_amplifier", send_amplifier);
        editor.putBoolean("send_antenna_power", send_antennaPower);
        editor.putString("send_file_name", send_filename);
        editor.putInt("send_sample_rate", send_sampleRate);
        editor.putInt("send_frequency", send_frequency);

        // morse transmission
        editor.putInt("morse_wpm", morse_wpm);
        editor.putInt("morse_frequency", morse_frequency);


        // rds
        editor.putInt("rds_audio_source", rds_audio_source);

        Log.d(LOGTAG, "Preferences saved: " + editor.commit());
    }

    public int getFFTSize() {
        return fftSize;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public int getSampleRate() {
        return sourceSamplerate;
    }

    public String getSourceFileName() {
        return getString("filesource_file_name", "");
    }

    public int getSourceFileFormat() {

        return filesourceFileFormat;
    }

    public boolean isRepeating() {
        return filesourceRepeat;
    }

    public boolean isExternalSource() {
        return rtlsrdExternalServer;
    }

    public int getVgaRxGain() {
        return vgaRxGain;
    }

    public boolean getAmplifier() {
        return hackrfAmplify;
    }

    public boolean getAntennaPower() {
        return hackrfAntennaPower;
    }

    public int getHackRfFrequenyShift() {
        return hackrfFrequencyShift;
    }

    public String getRtlSdrIp() {

        return rtlsdrIP;
    }

    public int getRtlSdrPort() {
        return rtlsdrPort;
    }

    public int getFrequencyCorrection() {

        return rtlsdrFrequencyCorrection;
    }

    public int getRtlsdrFrequencyShift() {
        return rtlsdrFrequencyShift;
    }

    public boolean isManualGain() {
        return rtlsdrManualGain;
    }

    public boolean isAGC() {
        return !isManualGain();
    }

    public int getGain() {

        return rtlsdrGain;
    }

    public int getIFGain() {
        return rtlsdrIfGain;
    }

    public int getLnaGain() {
        return lnaGain;
    }

    public boolean isRepeat() {
        return filesourceRepeat;
    }

    public void setRepeat(boolean repeat) {
        this.filesourceRepeat = repeat;
    }

    public boolean isAmp() {
        return hackrfAmplify;
    }

    public void setAmp(boolean amp) {
        this.hackrfAmplify = amp;
    }

    public String getIp() {
        return rtlsdrIP;
    }

    public void setIp(String ip) {
        this.rtlsdrIP = ip;
    }

    public Integer getPort() {
        return rtlsdrPort;
    }

    public void setPort(Integer port) {
        this.rtlsdrPort = port;
    }

    public boolean isExternalServer() {
        return rtlsrdExternalServer;
    }

    public void setExternalServer(boolean externalServer) {
        this.rtlsrdExternalServer = externalServer;
    }

    public Integer getRtlsdrFrequencyCorrection() {
        return rtlsdrFrequencyCorrection;
    }

    public void setRtlsdrFrequencyCorrection(Integer rtlsdrFrequencyCorrection) {
        this.rtlsdrFrequencyCorrection = rtlsdrFrequencyCorrection;
    }

    public Integer getHackrfFrequencyShift() {
        return hackrfFrequencyShift;
    }

    public void setHackrfFrequencyShift(Integer hackrfFrequencyShift) {
        this.hackrfFrequencyShift = hackrfFrequencyShift;
    }

    public void setFftSize(int fftSize) {
        this.fftSize = fftSize;
    }

    public boolean isAutomaticGainControl() {
        return !isManualGain();
    }

    public int getIfGain() {
        return rtlsdrIfGain;
    }

    public void setIfGain(int ifGain) {
        this.rtlsdrIfGain = ifGain;
    }

    public void setSampleRate(int sampleRate) {
        this.sourceSamplerate = sampleRate;
    }

    public void setSourceFileFormat(Integer sourceFileFormat) {
        this.filesourceFileFormat = sourceFileFormat;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void setAntennaPower(boolean antennaPower) {
        this.hackrfAntennaPower = antennaPower;
    }

    public void setRtlsdrFrequencyShift(Integer rtlsdrFrequencyShift) {
        this.rtlsdrFrequencyShift = rtlsdrFrequencyShift;
    }

    public void setVgaRxGain(int vgaRxGain) {
        this.vgaRxGain = vgaRxGain;
    }

    public void setManualGain(boolean manualGain) {
        this.rtlsdrManualGain = manualGain;
    }

    public void setGain(int gain) {
        this.rtlsdrGain = gain;
    }

    public void setLnaGain(int lnaGain) {
        this.lnaGain = lnaGain;
    }

    public boolean isRecordingStoppedAfterEnabled() {
        return recordingStopAfter;
    }

    public int getRecordingStoppedAfterValue() {
        return recordingStopAfterValue;
    }

    public int getRecordingStoppedAfterUnit() {
        return recordingStoppAfterUnit;
    }

    public void setRecordingStoppedAfterEnabled(boolean isRecordingStoppedAfterEnabled) {
        this.recordingStopAfter = isRecordingStoppedAfterEnabled;
    }

    public void setRecordingStoppedAfterValue(int recordingStoppedAfterValue) {
        this.recordingStopAfterValue = recordingStoppedAfterValue;
    }

    public void setRecordingStoppedAfterUnit(int recordingStoppedAfterUnit) {
        this.recordingStoppAfterUnit = recordingStoppedAfterUnit;
    }

    public int getRecordingSampleRate() {
        return recordingSampleRate;
    }

    public void setRecordingSampleRate(int recordingSampleRate) {
        this.recordingSampleRate = recordingSampleRate;
    }

    public boolean isAutostart() {
        return autostart;
    }

    public DemoType getDemodulation() {
        return demodulation;
    }

    public void setDemodulation(DemoType demodulation) {
        this.demodulation = demodulation;

    }

    public boolean isLogging() {
        return getBoolean("logging", false);
    }

    public File getLogFile() {
        return new File(getString("logfile", ""));
    }

    @Override
    public String getName() {
        return "misc";

    }

    @Override
    public int getResID() {
        return R.xml.misc_preferences;
    }

    public boolean isAdaptiveSamplerate() {
        return getBoolean("source_adaptive_samplerate", true);
    }

    public void setFileSourceFrequency(long frequency) {
        fileSourceFrequency = frequency;

    }

    public long getFileSourceFrequency() {
        return fileSourceFrequency;
    }

    public void setFileSourceSampleRate(int samplerate) {
        fileSourceSampleRate = samplerate;

    }

    public int getFileSourceSampleRate() {
        return fileSourceSampleRate;
    }

    public int getAverageSize() {
        return fftAveraging;
    }

    public int getSend_vgaGain() {
        return send_vgaGain;
    }

    public void setSend_vgaGain(int send_vgaGain) {
        this.send_vgaGain = send_vgaGain;
    }

    public boolean isSend_amplifier() {
        return send_amplifier;
    }

    public void setSend_amplifier(boolean send_amplifier) {
        this.send_amplifier = send_amplifier;
    }

    public boolean isSend_antennaPower() {
        return send_antennaPower;
    }

    public void setSend_antennaPower(boolean send_antennaPower) {
        this.send_antennaPower = send_antennaPower;
    }

    public int getSend_frequency() {
        return send_frequency;
    }

    public void setSend_frequency(int send_frequency) {
        this.send_frequency = send_frequency;
    }

    public int getSend_sampleRate() {
        return send_sampleRate;
    }

    public void setSend_sampleRate(int send_sampleRate) {
        this.send_sampleRate = send_sampleRate;
    }

    public String getSend_filename() {
        return send_filename;
    }

    public void setSend_filename(String send_filename) {
        this.send_filename = send_filename;
    }

    public Modulation.TxMode getSend_txMode() {
        return send_txMode;
    }

    public void setSend_txMode(Modulation.TxMode send_txMode) {
        this.send_txMode = send_txMode;
    }

    public String getSend_payloadText() {
        return send_payloadText;
    }

    public void setSend_payloadText(String send_payloadText) {
        this.send_payloadText = send_payloadText;
    }

    public int getMorse_frequency() {
        return morse_frequency;
    }

    public int getMorse_wpm() {
        return morse_wpm;
    }

    public int getMorse_DitDuration() {
        return (int) Math.round(1200d / morse_wpm);
    }

    public void setMorse_frequency(int morse_frequency) {
        this.morse_frequency = morse_frequency;
    }

    public void setMorse_wpm(int morse_wpm) {
        this.morse_wpm = morse_wpm;
    }

    public int getRds_audio_source() {
        return rds_audio_source;
    }

    public void setRds_audio_source(int rds_audio_source) {
        this.rds_audio_source = rds_audio_source;
    }
}
