-- Powód nieudanej analizy (status='failed'); NULL w pozostałych stanach. Kody = ReceiptFailureReason.code.
alter table receipts
  add column if not exists failure_reason text
    check (
      failure_reason is null
        or failure_reason in ('unsupported_format', 'not_a_receipt', 'unknown')
    );
