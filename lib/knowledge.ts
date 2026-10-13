import { catalog } from './catalogData';
import { PlantSearch } from './search';

/** The bundled encyclopedia and its search index (built once, works offline). */
export { catalog };
export const plantSearch = new PlantSearch(catalog);
