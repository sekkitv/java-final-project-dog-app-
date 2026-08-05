import L from 'leaflet';

const createCustomIcon = (emoji) => {
 return L.divIcon({
    html: `
      <div style="
        font-size: 28px;
        line-height: 1;
        filter: drop-shadow(0px 3px 4px rgba(0,0,0,0.4));
        cursor: pointer;
        user-select: none;
      ">
        ${emoji}
      </div>
    `,
    className: 'custom-clean-marker',
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -15],
  });
};

export const MAP_ICONS = {
  DOG_PARK: createCustomIcon('🐶'),  
  POOP_BAGS: createCustomIcon('💩'),
  WATER: createCustomIcon('💧'),
  SELECTED_LOCATION: createCustomIcon('🎯'),
  DEFAULT: createCustomIcon('📍'),
};

export const getHangoutIcon = (type) => {
  return MAP_ICONS[type] || MAP_ICONS.DEFAULT;
};