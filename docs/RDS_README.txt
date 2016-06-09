RDS Demodulator in AnSiAn
=========================

Alpha Release Notes
-------------------

The RDS Demodulator in AnSiAn is still in alpha state because it
is not working reliably. Main reason for this are:
- Performance: Older Android devices are not able to do the amount
  of computing and signal processing which is necessary to
  demodulate RDS. Filters and Downmixers are designed to be
  fast and not optimal.
- RDS demodulation algorithm is not optimal (again this is
  due to performance constraints).


Instructions
------------

The demodulator is implemented within the wideband FM demodulator.
It activates automatically once the wFM demodulation is started.
The output will be shown in the same view as the morse decoder,
right above the FFT. The left most number (#0000) indicates the
number of successfully demodulated frames and is an indicator how
well the current setup is working.

As mentioned above: The demodulation is unreliable. Please choose
strong and clean signals and try to vary tuning frequencies slightly
until frames are received.

For evaluation and testing, the demodulator can be operated with a
recorded file. Download the example file from:
http://wikisend.com/download/302532/2016-06-09-18-01-03_rtlsdr_100550000Hz_1000000Sps.iq
(sha1sum: fb31abc5dbb30b86228b9cce1759e07a78d91725)

Then go to Settings and choose FileSource. Go to Source Settings
and select the downloaded file as source file. All other settings
will be automatically done if the file is not renamed (parameters
are parsed from the filename).
Now start the demodulation and select wFM. Tune to the only strong
radio signal in the signal. Adjust the squelch bar so that the
audio is continously played. RDS information should appear on top
of the screen.


Known restrictions and open points
----------------------------------

- RDS shall be configurable in the settings:
  - deactivate RDS demodulation
  - Specify log file for RDS
- Performance shall be improved
  - Include error correction in algorithm
  - Improve signal processing
