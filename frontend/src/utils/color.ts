const PALETTE = [
  { bg: '#e6f4ff', border: '#91caff', text: '#0958d9' },
  { bg: '#f6ffed', border: '#b7eb8f', text: '#389e0d' },
  { bg: '#fff7e6', border: '#ffd591', text: '#d46b08' },
  { bg: '#f9f0ff', border: '#d3adf7', text: '#531dab' },
  { bg: '#fff1f0', border: '#ffa39e', text: '#cf1322' },
  { bg: '#e6fffb', border: '#87e8de', text: '#08979c' },
  { bg: '#fcffe6', border: '#eaff8f', text: '#7cb305' },
  { bg: '#fff0f6', border: '#ffadd2', text: '#c41d7f' },
]

export function courseColor(name: string) {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) % 100000
  }
  return PALETTE[hash % PALETTE.length]
}
