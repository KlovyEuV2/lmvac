config = {
    name = 'LuaInventoryPvP',
    cooldown = 'COOLDOWN'
}

receivingPackets = {
    USE_ENTITY
}

function onPacketReceiving(player, event)
    local packet = event:getPacket()
    local packetType = packet:getPacketType()

    local client = getClient(player)
    if client == nil then return end

    local uuid = tostring(player:getUniqueId())

    if client:isInventoryOpened() then
        event:setCancelled(true)
        flag(player,"Взаимодействия с сущностями в инвентаре!")
    end
end