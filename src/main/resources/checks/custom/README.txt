-----------------------------------------------
LMVAC - Помощь по Lua-чекам
-----------------------------------------------
/lmvlua - для доп. инфо о командах
lmvac.admin - для доступа к /lmvlua
-----------------------------------------------

Кидать скрипты в /plugins/LmvAC/checks/custom/lua/
Пример есть в данной папке.

-----------------------------------------------

flag(<игрок>,<причина>), для флага.
getConfig(), для конфига.
getClient(<игрок>), для получение анти-чит игрока.
runTaskSync(<function>) - Для вызова действия в главном тике сервера.
runTaskAsync(<function) - Для вызова дейстия в ансинхронном потоке.
runTaskLaterSync(<function>,<ticks>) - Для вызова действия через время.
log(<сообщение>) - Логирование в консоль.
sendMessage(<игрок>,<сообщение>) - Отправить игроку сообщение( можно использовать player:sendMessage(<сообщение>) )

-----------------------------------------------

receivingPackets = {
    'POSITION' # PacketType.Play.Client.<пакет>
} 

sendingPackets = {
    'POSITION' # PacketType.Play.Server.<пакет>
}

config = {
    name = 'Название Чека',
    cooldown = 'COOLDOWN' или 'NO_COOLDOWN', это будет ли на этот чек задержка в vl, есть 'NO_COOLDOWN', то будет засчитывать все жалобы, а если 'COOLDOWN' то только 1шт в 50мс. 
}

# пакеты игрока
function onPacketReceiving(player, event)
end

# пакеты сервера
function onPacketSending(player, event)
end

-----------------------------------------------
Для перезагрузки - /lmvlua reload

