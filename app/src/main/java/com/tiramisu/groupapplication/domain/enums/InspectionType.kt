package com.tiramisu.groupapplication.domain.enums

enum class InspectionType(val display: String, val category: String) {
    // HSE
    SAFETY_WALK("Safety Walk", "HSE"),
    PPE_COMPLIANCE("PPE Compliance", "HSE"),
    HOUSEKEEPING_5S("Housekeeping (5S)", "HSE"),
    EMERGENCY_EQUIPMENT("Emergency Equipment", "HSE"),
    EMERGENCY_DRILL("Emergency Drill", "HSE"),

    // Fire
    FIRE_EXTINGUISHER("Fire Extinguisher", "Fire"),
    FIRE_ALARM("Fire Alarm / Panel", "Fire"),
    SPRINKLER_SYSTEM("Sprinkler System", "Fire"),
    EGRESS_ROUTE("Egress / Exit Routes", "Fire"),
    FIRE_DOOR("Fire Door", "Fire"),

    // Electrical
    ELECTRICAL_PANEL("Electrical Panel / MCC", "Electrical"),
    PORTABLE_APPLIANCE("Portable Appliance (PAT)", "Electrical"),
    GROUNDING_BONDING("Grounding & Bonding", "Electrical"),
    LOTO_COMPLIANCE("LOTO Compliance", "Electrical"),
    ELECTRICAL_CABLES("Cables & Receptacles", "Electrical"),

    // Mechanical / Process
    MACHINE_GUARDING("Machine Guarding", "Mechanical"),
    PRESSURE_VESSEL("Pressure Vessel", "Mechanical"),
    BOILER("Boiler", "Mechanical"),
    LIFTING_EQUIPMENT("Lifting Equipment", "Mechanical"),
    COMPRESSED_AIR("Compressed Air System", "Mechanical"),
    CONVEYOR("Conveyor / Rotating Equipment", "Mechanical"),

    // Chemical / Lab
    CHEMICAL_STORAGE("Chemical Storage", "Chemical"),
    SDS_AVAILABILITY("SDS Availability", "Chemical"),
    SPILL_CONTAINMENT("Spill Kits & Containment", "Chemical"),
    FUME_HOOD("Fume Hood / LEV", "Chemical"),
    GAS_CYLINDER("Gas Cylinder Handling", "Chemical"),

    // Environmental
    WASTE_MANAGEMENT("Waste Management", "Environmental"),
    WASTEWATER_DISCHARGE("Wastewater / Effluent", "Environmental"),
    STORMWATER("Stormwater / SPCC", "Environmental"),
    AIR_EMISSIONS("Air Emissions", "Environmental"),
    NOISE_DUST("Noise / Dust", "Environmental"),
    ENERGY_SUSTAIN("Energy & Sustainability", "Environmental"),

    // Food Safety / GMP
    GMP_HYGIENE("GMP Hygiene", "Food/GMP"),
    SANITATION("Sanitation", "Food/GMP"),
    TEMPERATURE_CONTROL("Temperature Control", "Food/GMP"),
    ALLERGEN_CONTROL("Allergen Control", "Food/GMP"),
    PEST_CONTROL("Pest Control", "Food/GMP"),
    HACCP_CCP("HACCP CCP Check", "Food/GMP"),

    // Construction
    SCAFFOLDING("Scaffolding", "Construction"),
    EXCAVATION_TRENCHING("Excavation / Trenching", "Construction"),
    WORKING_AT_HEIGHT("Working at Height", "Construction"),
    HOT_WORK("Hot Work", "Construction"),
    CONFINED_SPACE("Confined Space", "Construction"),
    TRAFFIC_MANAGEMENT("Site Traffic Management", "Construction"),
    RIGGING("Rigging / Lifting Plan", "Construction"),

    // Facility / Building
    HVAC("HVAC", "Facility"),
    PLUMBING("Plumbing", "Facility"),
    LIGHTING("Lighting", "Facility"),
    ROOF_STRUCTURE("Roof / Structure", "Facility"),
    ELEVATOR_LIFT("Elevator / Lift", "Facility"),
    ACCESS_SECURITY("Access & Security", "Facility"),

    // Warehouse / Fleet / Office
    RACKING_STORAGE("Racking & Storage", "Warehouse"),
    FORKLIFT("Forklift / PIT", "Warehouse"),
    VEHICLE_PRETRIP("Vehicle Pre-Trip", "Fleet"),
    OFFICE_ERGONOMICS("Office Ergonomics (DSE)", "Office"),
    FIRST_AID("First Aid Kits & Stations", "Health"),

    // Public
    PLAYGROUND("Playground Safety", "Public"),
    SWIMMING_POOL("Swimming Pool Safety", "Public"),

    // General
    SITE_GENERAL("Site General", "General"),
    OTHER("Other / Custom", "General")
}