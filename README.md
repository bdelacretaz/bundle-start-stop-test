bundle-start-stop-test
======================

Utility to semi-randomly stop and restart OSGi bundles, for stress tests.

Simply install and start this bundle, and watch the logs to see what's
happening. A thread is started to randomly stop and restart many bundles,
omitting those that contain some predefined patterns in their symbolic 
names to avoid taking the framework down.
