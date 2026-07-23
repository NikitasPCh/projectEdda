package com.edda.server.repository;

import com.edda.server.entity.ActionPrimaryReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionPrimaryRewardRepository extends JpaRepository<ActionPrimaryReward, String> {
}