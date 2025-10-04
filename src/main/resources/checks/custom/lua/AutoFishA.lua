config = {
    name = 'AutoFishA',
    cooldown = 'COOLDOWN'
}

receivingPackets = {
    'USE_ITEM'
}

function onPacketReceiving(player, event)
    local config = getConfig()
    local client = getClient(player)
    local uuid = player:getUniqueId()

    local time = getTime()

    if client == nil then
        return
    end

    if time-client.lastArmAnimation > 20 then
        event:setCancelled(true)
        flag(player)
    end
end
