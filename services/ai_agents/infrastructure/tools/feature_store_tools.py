"""Feature Store tools for AI Infrastructure Agents.

This module provides tools for feature engineering, feature management, and feature serving.
"""

from datetime import datetime
from typing import Any, Dict, List, Optional

from langchain_core.tools import BaseTool
from pydantic import BaseModel, Field

from services.ai_agents.infrastructure.schemas import (
    FeatureDefinition,
    FeatureGroup,
    FeatureMaterializationConfig,
    FeatureTransformation,
    FeatureVector,
    FeatureValue,
)
from services.ai_agents.infrastructure.adapters.feature_store_adapter import FeatureStoreAdapter as DomainFeatureStoreAdapter


# ============================================================================
# Tool Input Schemas
# ============================================================================


class CreateFeatureGroupInput(BaseModel):
    """Input schema for creating a feature group."""
    name: str = Field(..., description="Feature group name")
    entities: List[str] = Field(..., description="Entity identifiers")
    features: List[FeatureDefinition] = Field(..., description="Feature definitions")
    description: Optional[str] = Field(default=None)
    online_enabled: bool = Field(default=True)


class RegisterFeatureInput(BaseModel):
    """Input schema for registering a feature."""
    feature_group: str = Field(..., description="Feature group name")
    feature_name: str = Field(..., description="Feature name")
    dtype: str = Field(..., description="Data type")
    description: Optional[str] = None


class GetFeatureVectorInput(BaseModel):
    """Input schema for getting a feature vector."""
    feature_group: str = Field(..., description="Feature group name")
    entity_id: str = Field(..., description="Entity ID")
    features: List[str] = Field(..., description="Feature names to retrieve")


class MaterializeFeaturesInput(BaseModel):
    """Input schema for feature materialization."""
    feature_group: str = Field(..., description="Feature group name")
    start_time: str = Field(..., description="Start time (ISO format)")
    end_time: Optional[str] = Field(default=None, description="End time (ISO format)")


class CreateTransformationInput(BaseModel):
    """Input schema for creating a feature transformation."""
    name: str = Field(..., description="Transformation name")
    input_features: List[str] = Field(..., description="Input feature names")
    transformation_type: str = Field(..., description="Type: sql, python, udf")
    code: Optional[str] = Field(default=None, description="Transformation code")


# ============================================================================
# Feature Store Adapter Wrapper (translates between domain and infrastructure schemas)
# ============================================================================


class FeatureStoreAdapter:
    """Adapter for Feature Store operations.
    
    This is a wrapper around the domain FeatureStoreAdapter that translates between
    infrastructure schemas (FeatureDefinition, FeatureGroup) and domain types.
    """
    
    def __init__(self):
        """Initialize the Feature Store adapter."""
        self._domain_adapter = DomainFeatureStoreAdapter()
        self._feature_groups: Dict[str, Dict[str, Any]] = {}
        self._feature_values: Dict[str, Dict[str, Any]] = {}
        self._transformations: Dict[str, Dict[str, Any]] = {}
        self._materialization_jobs: Dict[str, Dict[str, Any]] = {}
    
    def create_feature_group(
        self,
        name: str,
        entities: List[str],
        features: List[FeatureDefinition],
        description: Optional[str] = None,
        online_enabled: bool = True
    ) -> Dict[str, Any]:
        """Create a feature group."""
        schema = [
            {"name": f.name, "dtype": f.dtype, "description": f.description, "entity": entities[0] if entities else "default"}
            for f in features
        ]
        self._domain_adapter.create_feature_group(name, "v1", schema)
        
        self._feature_groups[name] = {
            "name": name,
            "entities": entities,
            "features": [f.model_dump() for f in features],
            "description": description,
            "online_enabled": online_enabled
        }
        
        return {"success": True, "feature_group": name, "entities": entities, "features": len(features)}
    
    def get_feature_group(self, name: str) -> Optional[FeatureGroup]:
        """Get a feature group."""
        if name not in self._feature_groups:
            return None
        return FeatureGroup.model_validate(self._feature_groups[name])
    
    def list_feature_groups(self) -> List[FeatureGroup]:
        """List all feature groups."""
        return [FeatureGroup.model_validate(g) for g in self._feature_groups.values()]
    
    def register_feature(
        self,
        feature_group: str,
        feature_name: str,
        dtype: str,
        description: Optional[str] = None
    ) -> Dict[str, Any]:
        """Register a new feature."""
        if feature_group not in self._feature_groups:
            return {"success": False, "error": f"Feature group '{feature_group}' not found"}
        
        new_feature = FeatureDefinition(name=feature_name, dtype=dtype, description=description)
        self._feature_groups[feature_group]["features"].append(new_feature.model_dump())
        
        return {"success": True, "feature_group": feature_group, "feature": feature_name}
    
    def get_feature_vector(
        self,
        feature_group: str,
        entity_id: str,
        features: List[str]
    ) -> Optional[FeatureVector]:
        """Get a feature vector for an entity."""
        domain_vector = self._domain_adapter.get_feature_vector(feature_group, entity_id, features)
        
        if domain_vector:
            return FeatureVector(
                entity_id=domain_vector.entity_id,
                entity_type=domain_vector.entity_type,
                features=[
                    FeatureValue(name=fv.name, value=fv.value) for fv in domain_vector.features
                ]
            )
        
        return None
    
    def write_features(
        self,
        feature_group: str,
        entity_id: str,
        features: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Write feature values."""
        self._domain_adapter.write_features(feature_group, entity_id, features)
        
        key = f"{feature_group}:{entity_id}"
        self._feature_values[key] = {
            "feature_group": feature_group,
            "entity_id": entity_id,
            "features": features,
            "timestamp": datetime.now().isoformat()
        }
        
        return {"success": True, "entity_id": entity_id, "features_written": len(features)}
    
    def materialize_features(
        self,
        feature_group: str,
        start_time: datetime,
        end_time: Optional[datetime] = None
    ) -> Dict[str, Any]:
        """Materialize features to the offline store."""
        result = self._domain_adapter.materialize_features(feature_group, start_time, end_time)
        
        return {"success": result.get("success", True), "job_id": result.get("job_id"), "rows_materialized": result.get("rows_materialized", 1000)}
    
    def create_transformation(
        self,
        name: str,
        input_features: List[str],
        transformation_type: str,
        code: Optional[str] = None
    ) -> Dict[str, Any]:
        """Create a feature transformation."""
        self._domain_adapter.create_transformation(name, input_features, transformation_type, code)
        
        transformation = FeatureTransformation(
            name=name,
            input_features=input_features,
            transformation_type=transformation_type,
            code=code,
            output_type="float"
        )
        self._transformations[name] = transformation.model_dump()
        
        return {"success": True, "transformation": name}
    
    def list_transformations(self) -> List[FeatureTransformation]:
        """List all transformations."""
        return [FeatureTransformation.model_validate(t) for t in self._transformations.values()]


# Global adapter instance
_feature_store_adapter: Optional[FeatureStoreAdapter] = None


def get_feature_store_adapter() -> FeatureStoreAdapter:
    """Get the global Feature Store adapter instance."""
    global _feature_store_adapter
    if _feature_store_adapter is None:
        _feature_store_adapter = FeatureStoreAdapter()
    return _feature_store_adapter


# ============================================================================
# Feature Store Tools
# ============================================================================


def _create_feature_group_tool() -> BaseTool:
    """Create the create feature group tool."""
    def create_feature_group(
        name: str,
        entities: List[str],
        feature_definitions: List[Dict[str, str]],
        description: Optional[str] = None,
        online_enabled: bool = True
    ) -> str:
        """Create a new feature group.
        
        Args:
            name: Feature group name.
            entities: Entity identifiers.
            feature_definitions: Feature definitions list.
            description: Optional description.
            online_enabled: Enable online serving.
            
        Returns:
            Creation result.
        """
        adapter = get_feature_store_adapter()
        
        features = [
            FeatureDefinition(name=f["name"], dtype=f["dtype"], description=f.get("description"))
            for f in feature_definitions
        ]
        
        result = adapter.create_feature_group(
            name=name,
            entities=entities,
            features=features,
            description=description,
            online_enabled=online_enabled
        )
        
        if result["success"]:
            return f"""Successfully created feature group '{name}'
Entities: {', '.join(entities)}
Features: {result['features']}
Online Enabled: {online_enabled}"""
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="create_feature_group",
        description="Create a new feature group. Use this to define a collection of related features for an entity.",
        args_schema=CreateFeatureGroupInput,
        func=create_feature_group
    )


def _create_register_feature_tool() -> BaseTool:
    """Create the register feature tool."""
    def register_feature(
        feature_group: str,
        feature_name: str,
        dtype: str,
        description: Optional[str] = None
    ) -> str:
        """Register a new feature.
        
        Args:
            feature_group: Feature group name.
            feature_name: Feature name.
            dtype: Data type.
            description: Optional description.
            
        Returns:
            Registration result.
        """
        adapter = get_feature_store_adapter()
        result = adapter.register_feature(feature_group, feature_name, dtype, description)
        
        if result["success"]:
            return f"Successfully registered feature '{feature_name}' in group '{feature_group}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="register_feature",
        description="Register a new feature in an existing feature group. Use this to add new features to track.",
        args_schema=RegisterFeatureInput,
        func=register_feature
    )


def _create_get_feature_vector_tool() -> BaseTool:
    """Create the get feature vector tool."""
    def get_feature_vector(
        feature_group: str,
        entity_id: str,
        features: List[str]
    ) -> str:
        """Get a feature vector for an entity.
        
        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature names to retrieve.
            
        Returns:
            Feature vector.
        """
        adapter = get_feature_store_adapter()
        vector = adapter.get_feature_vector(feature_group, entity_id, features)
        
        if vector is None:
            return f"No feature vector found for entity '{entity_id}' in group '{feature_group}'"
        
        output = f"Feature Vector for {entity_id}:\n"
        for fv in vector.features:
            output += f"  - {fv.name}: {fv.value}\n"
        output += f"Timestamp: {vector.timestamp.isoformat()}\n"
        
        return output
    
    return BaseTool(
        name="get_feature_vector",
        description="Get a feature vector for an entity. Use this to retrieve pre-computed features for ML models.",
        args_schema=GetFeatureVectorInput,
        func=get_feature_vector
    )


def _create_write_features_tool() -> BaseTool:
    """Create the write features tool."""
    def write_features(
        feature_group: str,
        entity_id: str,
        features: Dict[str, Any]
    ) -> str:
        """Write feature values.
        
        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature values to write.
            
        Returns:
            Write result.
        """
        adapter = get_feature_store_adapter()
        result = adapter.write_features(feature_group, entity_id, features)
        
        if result["success"]:
            return f"Successfully wrote {result['features_written']} features for entity '{entity_id}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="write_features",
        description="Write feature values for an entity. Use this to store computed feature values.",
        args_schema={
            "feature_group": str,
            "entity_id": str,
            "features": Dict[str, Any]
        },
        func=write_features
    )


def _create_materialize_features_tool() -> BaseTool:
    """Create the materialize features tool."""
    def materialize_features(
        feature_group: str,
        start_time: str,
        end_time: Optional[str] = None
    ) -> str:
        """Materialize features to the offline store.
        
        Args:
            feature_group: Feature group name.
            start_time: Start time (ISO format).
            end_time: Optional end time (ISO format).
            
        Returns:
            Materialization result.
        """
        adapter = get_feature_store_adapter()
        
        start = datetime.fromisoformat(start_time.replace("Z", "+00:00"))
        end = None
        if end_time:
            end = datetime.fromisoformat(end_time.replace("Z", "+00:00"))
        
        result = adapter.materialize_features(feature_group, start, end)
        
        if result["success"]:
            return f"""Feature materialization completed!
Job ID: {result['job_id']}
Feature Group: {feature_group}
Rows Materialized: {result['rows_materialized']}"""
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="materialize_features",
        description="Materialize features to the offline store. Use this to backfill historical features for training.",
        args_schema=MaterializeFeaturesInput,
        func=materialize_features
    )


def _create_list_feature_groups_tool() -> BaseTool:
    """Create the list feature groups tool."""
    def list_feature_groups() -> str:
        """List all feature groups.
        
        Returns:
            List of feature groups.
        """
        adapter = get_feature_store_adapter()
        groups = adapter.list_feature_groups()
        
        if not groups:
            return "No feature groups found"
        
        output = f"Feature Groups ({len(groups)}):\n\n"
        for g in groups:
            output += f"- {g.name}\n"
            output += f"  Entities: {', '.join(g.entities)}\n"
            output += f"  Features: {len(g.features)}\n"
            output += f"  Online: {'Yes' if g.online_enabled else 'No'}\n"
        
        return output
    
    return BaseTool(
        name="list_feature_groups",
        description="List all available feature groups. Use this to see what feature collections are available.",
        args_schema={},
        func=list_feature_groups
    )


def _create_create_transformation_tool() -> BaseTool:
    """Create the create transformation tool."""
    def create_transformation(
        name: str,
        input_features: List[str],
        transformation_type: str,
        code: Optional[str] = None
    ) -> str:
        """Create a feature transformation.
        
        Args:
            name: Transformation name.
            input_features: Input feature names.
            transformation_type: Type of transformation.
            code: Transformation code.
            
        Returns:
            Creation result.
        """
        adapter = get_feature_store_adapter()
        result = adapter.create_transformation(name, input_features, transformation_type, code)
        
        if result["success"]:
            return f"Successfully created transformation '{name}' with type '{transformation_type}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="create_transformation",
        description="Create a feature transformation. Use this to define custom feature engineering logic.",
        args_schema=CreateTransformationInput,
        func=create_transformation
    )


def get_all_feature_store_tools() -> List[BaseTool]:
    """Get all Feature Store tools.
    
    Returns:
        List of all Feature Store tools.
    """
    return [
        _create_feature_group_tool(),
        _create_register_feature_tool(),
        _create_get_feature_vector_tool(),
        _create_write_features_tool(),
        _create_materialize_features_tool(),
        _create_list_feature_groups_tool(),
        _create_create_transformation_tool(),
    ]
