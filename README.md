muvr-analytics
====
Murv is an application that uses _wearable devices_ (Pebble)—in combination with a mobile app—to submit 
physical (i.e. accelerometer, compass) and biological (i.e. heart rate) information to a CQRS/ES cluster to be
analysed.

---
muvr-analytics contains analytics pipelines external to the main application, including
* pipelines to suggest future exercise sessions
* pipelines to classify users to groups by attribute similarity
* pipelines to improve classification and other models