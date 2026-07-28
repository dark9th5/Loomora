export const featureMatrix = [
  ['Recording, playback and library', 'Available', 'No account or model required'],
  ['Non-destructive editor basics', 'Beta', 'Codec behavior varies by device'],
  ['Offline transcription', 'Beta', 'Requires imported sherpa Whisper multilingual model pack'],
  ['Speaker diarization', 'Beta', 'Generic labels only; no identity inference'],
  ['Evidence-linked extractive insights', 'Available', 'Runs locally from transcript segments'],
  ['Deep generative LLM summaries', 'Coming Soon', 'Not accepted as release-ready'],
] as const;

export const adminModules = [
  'Customers',
  'Users and roles',
  'Licenses',
  'License issuance',
  'Products and editions',
  'Capabilities',
  'Orders',
  'Manual payment confirmation',
  'App releases and downloads',
  'Support tickets',
  'Contact leads',
  'Blog/content workflow',
  'Audit logs',
  'Settings',
];

export const customerModules = [
  'License status',
  'Edition and capabilities',
  'Issue and expiry dates',
  'Device binding status',
  'Latest app release',
  'Recent order',
  'Open tickets',
  'Download actions',
];
