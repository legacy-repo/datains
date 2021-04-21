--;;
CREATE TABLE IF NOT EXISTS datains_message (
  id SERIAL NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  message_type VARCHAR(32) NOT NULL,
  payload JSONB NOT NULL,
  created_time BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL
);

--;;
COMMENT ON TABLE datains_message IS 'Used for managing message.';

--;;
COMMENT ON COLUMN datains_message.id IS 'serial id for report.';

--;;
COMMENT ON COLUMN datains_message.title IS 'The title of message.';

--;;
COMMENT ON COLUMN datains_message.description IS 'A description of message.';

--;;
COMMENT ON COLUMN datains_message.payload IS 'More details with message.';

--;;
COMMENT ON COLUMN datains_message.message_type IS 'Which type the message is.';

--;;
COMMENT ON COLUMN datains_message.status IS 'Submitted, Running, Succeeded.';