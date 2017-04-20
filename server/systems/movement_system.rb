class MovementSystem
  def update(entity_manager, dt, input, res)
    # TODO add unit speed
    tile_size = RtsGame::TILE_SIZE
    speed = tile_size.to_f/(RtsGame::TURN_DURATION * 5)

    # TODO update tile info on unit creation
    tile_infos =  {} 
    entity_manager.each_entity(PlayerOwned, TileInfo) do |ent|
      player, tile_info = ent.components
      tile_infos[player.id] = tile_info
    end
      
    # TODO what about non-player owned movement? sep query?
    entity_manager.each_entity PlayerOwned, Unit, MovementCommand, Position do |ent|
      pwn, u, movement, pos = ent.components
      ent_id = ent.id

      dir = movement.target_vec - pos.to_vec
      move = dir.unit * dt * speed
      pos.x += move.x
      pos.y += move.y

      # TODO detect crossover of target point
      if (movement.target_vec - pos.to_vec).magnitude < 3
        pos.x = movement.target_vec.x.round
        pos.y = movement.target_vec.y.round

        base_ent = entity_manager.find(Base, PlayerOwned, Position, Label).select{|ent| ent.get(PlayerOwned).id == pwn.id}.first
        base_pos = base_ent.get(Position)

        if (base_pos.x - pos.x).abs <= 1 && (base_pos.y - pos.y).abs <= 1
          base = base_ent.get(Base)
          unit_res_ent = entity_manager.find_by_id(ent_id, ResourceCarrier, Label)
          if unit_res_ent
            unit_res, unit_label = unit_res_ent.components
            base.resource += unit_res.resource
            base_ent.get(Label).text = base.resource
            unit_res.resource = 0
            unit_label.text = ""
          end
        end

        # TODO update for visible range
        range = 3
        TileInfoHelper.update_tile_visibility(tile_infos[pwn.id], pos.x, pos.y, range)
        u.status = :idle
        # XXX ¯\_(ツ)_/¯ can I do this safely while iterating?
        entity_manager.remove_component(klass: MovementCommand, id: ent_id)
      end
    end
  end
end

