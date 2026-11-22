import type { PricingLineInput } from '../api';

export const emptyLine = (): PricingLineInput => ({ service: '', standardPrice: '', proposedPrice: '', committedVolume: 1 });

interface Props {
  line: PricingLineInput;
  onChange: (line: PricingLineInput) => void;
}

/** One editable pricing line. Prices stay strings end to end so nothing is rounded in JS. */
export function PricingLineFields({ line, onChange }: Props) {
  return (
    <>
      <input
        aria-label="Service"
        placeholder="Service (e.g. Wire transfer)"
        value={line.service}
        onChange={(e) => onChange({ ...line, service: e.target.value })}
        required
      />
      <input
        aria-label="Standard price"
        placeholder="Standard price"
        inputMode="decimal"
        pattern="\d+(\.\d{1,4})?"
        value={line.standardPrice}
        onChange={(e) => onChange({ ...line, standardPrice: e.target.value })}
        required
      />
      <input
        aria-label="Proposed price"
        placeholder="Proposed price"
        inputMode="decimal"
        pattern="\d+(\.\d{1,4})?"
        value={line.proposedPrice}
        onChange={(e) => onChange({ ...line, proposedPrice: e.target.value })}
        required
      />
      <input
        aria-label="Committed annual volume"
        type="number"
        min={1}
        step={1}
        value={line.committedVolume}
        onChange={(e) => onChange({ ...line, committedVolume: Number(e.target.value) })}
        required
      />
    </>
  );
}
