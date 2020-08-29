--;;
ALTER TABLE datains_report
ADD COLUMN report_id VARCHAR(36);

--;;
COMMENT ON COLUMN datains_report.report_id IS 'uuid for report from tservice';