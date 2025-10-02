config = {
    name = 'LuaInventoryF',
    cooldown = 'COOLDOWN'
}

receivingPackets = {
    'POSITION'
}

local moves = {}

function onPacketReceiving(player, event)
    local config = getConfig()
    local client = getClient(player)
    local uuid = player:getUniqueId()

    if client == nil then
        return
    end

    local currentMoves = moves[uuid] or 0

    if client.isInventoryOpened and player:isSprinting() then
        currentMoves = currentMoves + 1
        moves[uuid] = currentMoves
        if currentMoves >= 2 then
            runTaskSync(function()
                player:closeInventory()
            end)
            flag(player, "Бег в инвентаре")
        end
    else
        moves[uuid] = -1
    end
end
