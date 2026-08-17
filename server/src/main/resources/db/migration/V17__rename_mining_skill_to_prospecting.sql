INSERT INTO game.skill (key, name) VALUES ('prospecting', 'Prospecting');

UPDATE game.character_skill SET skill_key = 'prospecting' WHERE skill_key = 'mining';

DELETE FROM game.skill WHERE key = 'mining';