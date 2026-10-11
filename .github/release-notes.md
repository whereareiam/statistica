---
version: 1.0.0
---

# Statistica 1.0.0 - First release

Statistica sends a plugin's usage statistics to bStats, FastStats or both from one declaration of its metrics, and reports its errors to FastStats without player data.

## Highlights

* Metrics declared once become bStats charts and FastStats values; counters stay exact across both services.
* Shared totals are sent by one elected instance, through a database lease, a Redis lease, or always for a standalone instance.
* Error reports only cover the plugin's own code and hide the values a plugin names, unique ids, e-mail addresses and database values.
