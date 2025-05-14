export const graphStyleConfig = {
  width: 800,
  height: 600,
  nodeRadius: 6,
  nodeColor: '#4299E1',
  sourceNodeColor: '#F6E05E',
  daoNodeColor: '#68D391',
  currentNodeColor: '#ED8936',
  libraryNodeColor: '#A0AEC0',
  linkColor: '#A0AEC0',
  edgeColors: {
    calls: '#4299E1',
    implemented_by: '#38A169',
    overridden_by: '#E53E3E',
    out_calls: '#A0AEC0',
    super_calls: '#805AD5',
    interface_calls: '#DD6B20',
    subtype_calls: '#3182CE',
    injection_calls: '#D53F8C',
    contains: '#4A5568',
    depends_on: '#2B6CB0',
    documented_by: '#718096',
    instance_of: '#D69E2E',
    reads_field: '#0BC5EA',
    writes_field: '#319795',
    maps_to: '#9F7AEA',
    passes_to: '#DD6B20'
  },
  margin: { top: 20, right: 90, bottom: 20, left: 90 },
  arrowSize: 5,
  animation: {
    enabled: true,
    intensity: 0.03,
    duration: 300
  }
};

export const defaultEdgePalette = ['#4299E1', '#38A169', '#E53E3E', '#805AD5', '#DD6B20', '#3182CE', '#D53F8C', '#4A5568', '#2B6CB0', '#C05621'];
