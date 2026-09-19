# fuse

Одноразовый прототип FUSE-ФС на jnr-fuse (libfuse 2): логирует каждый вызов и каждый записанный байт. Формат файла не разбирается — анализ делается грепом по логу.

## Сборка и запуск
Образ `amazoncorretto:21`: `yum install -y findutils fuse-libs` (findutils — для `gradlew`, fuse-libs — `libfuse.so.2`).
Контейнер: `--device /dev/fuse --cap-add SYS_ADMIN` (+ `--security-opt apparmor=unconfined` при AppArmor). Программа-писатель должна работать в том же контейнере — снаружи точка монтирования не видна.
```
./gradlew shadowJar
java -jar build/libs/fuse.jar /mnt/spy   # добавляй -d, -o allow_other, -o nonempty при необходимости
fusermount -u /mnt/spy || umount /mnt/spy   # размонтирование
```
Вне контейнера на Ubuntu: `libfuse2` (22.04) или `libfuse2t64` (24.04+), не `fuse` (конфликтует с fuse3).
Писатель от другого пользователя/root — нужен `-o allow_other` и раскомментированный `user_allow_other` в `/etc/fuse.conf`.

## Лог
Консоль и `./fuse.log`. Уровень — `root level` в `src/main/resources/logback.xml`: INFO — сводка вызовов, DEBUG — плюс hexdump записей, TRACE — плюс `getattr`.
