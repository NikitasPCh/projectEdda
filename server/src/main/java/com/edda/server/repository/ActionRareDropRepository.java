package com.edda.server.repository;

import com.edda.server.entity.ActionRareDrop;
import com.edda.server.entity.ActionRareDropId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionRareDropRepository extends JpaRepository<ActionRareDrop, ActionRareDropId> {
    List<ActionRareDrop> findByIdActionKey(String actionKey);
}