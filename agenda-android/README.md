# Agenda — widget de calendar pentru Android

Widget de ecran pentru Google Calendar, în stilul Steluței: fundal translucid reglabil
(0–100%), colțuri rotunjite de 28dp, contur fin „de sticlă" și listă care se estompează
la margini când o derulezi.

- Citește calendarele sincronizate pe telefon (`CalendarContract`) — fără cont, cheie de
  API sau internet. Doar calendarele vizibile; invitațiile refuzate nu apar.
- Atingi un eveniment → se deschide în Google Calendar. Data → calendarul pe azi. **+** →
  eveniment nou.
- Se actualizează singur când se schimbă ceva în calendar (`CalendarWatchJob`), când se
  termină un eveniment de azi și la miezul nopții.
- Din aplicație: opacitate, ton închis/deschis, contur, câte zile înainte (3/7/14/30).

APK-ul îl face GitHub Actions (`.github/workflows/agenda-apk.yml`) la fiecare push în
`agenda-android/` și îl pune la Releases. Local: `gradlew.bat assembleDebug`, ca la Steluța.
