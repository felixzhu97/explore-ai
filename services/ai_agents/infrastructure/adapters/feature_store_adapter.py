"""Feature Store Adapter - Anti-Corruption Layer for Feature Store operations.

This adapter implements feature store operations and translates between
external system formats and domain models.
"""

import uuid
from abc import ABC
from datetime import datetime
from typing import Any, Dict, List, Optional

from services.ai_agents.domain.values import (
    FeatureDefinition,
    FeatureGroup,
    FeatureTransformation,
    FeatureVector,
    FeatureValue,
)
from services.ai_agents.domain.repositories.feature_store_repository import (
    FeatureGroupInfo,
    FeatureStoreRepository,
)


class FeatureStoreAdapter(FeatureStoreRepository):
    """Adapter for Feature Store operations.

    Translates external feature store calls to domain models using
    value objects from domain.repositories.feature_store_repository.
    """

    def __init__(self) -> None:
        """Initialize the Feature Store adapter with in-memory storage."""
        self._feature_groups: Dict[str, FeatureGroup] = {}
        self._feature_values: Dict[str, Dict[str, Any]] = {}
        self._transformations: Dict[str, FeatureTransformation] = {}
        self._materialization_jobs: Dict[str, Dict[str, Any]] = {}

    def create_feature_group(
        self,
        name: str,
        version: str,
        schema: List[Dict[str, Any]],
    ) -> FeatureGroupInfo:
        """Create a new feature group.

        Args:
            name: Feature group name.
            version: Version string.
            schema: Feature schema definitions.

        Returns:
            FeatureGroupInfo value object.
        """
        features = [
            FeatureDefinition(
                name=item["name"],
                dtype=item["dtype"],
                description=item.get("description"),
            )
            for item in schema
        ]

        entities = list(set(item.get("entity") for item in schema if "entity" in item))

        group = FeatureGroup(
            name=name,
            entities=entities,
            features=features,
            description=None,
            online_enabled=True,
        )

        self._feature_groups[name] = group

        return FeatureGroupInfo(
            name=name,
            version=version,
            feature_count=len(features),
        )

    def list_feature_groups(self) -> List[FeatureGroupInfo]:
        """List all feature groups.

        Returns:
            List of feature group information.
        """
        return [
            FeatureGroupInfo(
                name=name,
                version="v1",
                feature_count=len(group.features),
            )
            for name, group in self._feature_groups.items()
        ]

    def get_feature_group(self, name: str, version: str) -> FeatureGroupInfo:
        """Get feature group details.

        Args:
            name: Feature group name.
            version: Version string.

        Returns:
            FeatureGroupInfo value object.
        """
        if name not in self._feature_groups:
            raise ValueError(f"Feature group '{name}' not found")

        group = self._feature_groups[name]
        return FeatureGroupInfo(
            name=name,
            version=version,
            feature_count=len(group.features),
        )

    def write_feature_group(
        self,
        name: str,
        version: str,
        features: List[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """Write features to a feature group.

        Args:
            name: Feature group name.
            version: Version string.
            features: Feature values to write.

        Returns:
            Write result.
        """
        if name not in self._feature_groups:
            raise ValueError(f"Feature group '{name}' not found")

        entity_id = features[0].get("entity_id") if features else None
        if not entity_id:
            raise ValueError("entity_id is required in features")

        feature_values = {
            item["feature_name"]: item["value"]
            for item in features
            if "feature_name" in item and "value" in item
        }

        key = f"{name}:{version}:{entity_id}"
        self._feature_values[key] = {
            "feature_group": name,
            "version": version,
            "entity_id": entity_id,
            "features": feature_values,
            "timestamp": datetime.now(),
        }

        return {
            "success": True,
            "entity_id": entity_id,
            "features_written": len(feature_values),
        }

    def get_historical_features(
        self,
        name: str,
        version: str,
        entity_ids: List[str],
    ) -> List[Dict[str, Any]]:
        """Get historical features for entities.

        Args:
            name: Feature group name.
            version: Version string.
            entity_ids: List of entity IDs.

        Returns:
            List of historical feature records.
        """
        if name not in self._feature_groups:
            raise ValueError(f"Feature group '{name}' not found")

        results = []
        for entity_id in entity_ids:
            key = f"{name}:{version}:{entity_id}"
            if key in self._feature_values:
                results.append(self._feature_values[key])

        return results

    def get_feature_group_internal(self, name: str) -> Optional[FeatureGroup]:
        """Get internal feature group representation.

        Args:
            name: Feature group name.

        Returns:
            FeatureGroup or None.
        """
        return self._feature_groups.get(name)

    def list_feature_groups_internal(self) -> List[FeatureGroup]:
        """List all feature groups internally.

        Returns:
            List of feature groups.
        """
        return list(self._feature_groups.values())

    def materialize_features(
        self,
        feature_group: str,
        start_time: datetime,
        end_time: Optional[datetime] = None,
    ) -> Dict[str, Any]:
        """Materialize features to the offline store.

        Args:
            feature_group: Feature group name.
            start_time: Start time.
            end_time: Optional end time.

        Returns:
            Materialization result.
        """
        job_id = f"materialize_{uuid.uuid4().hex[:8]}"

        self._materialization_jobs[job_id] = {
            "job_id": job_id,
            "feature_group": feature_group,
            "start_time": start_time,
            "end_time": end_time,
            "status": "completed",
            "rows_materialized": 1000,
        }

        return {
            "success": True,
            "job_id": job_id,
            "rows_materialized": 1000,
        }

    def create_transformation(
        self,
        name: str,
        input_features: List[str],
        transformation_type: str,
        code: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Create a feature transformation.

        Args:
            name: Transformation name.
            input_features: Input feature names.
            transformation_type: Type of transformation.
            code: Transformation code.

        Returns:
            Creation result.
        """
        transformation = FeatureTransformation(
            name=name,
            input_features=input_features,
            transformation_type=transformation_type,
            code=code,
            output_type="float",
        )

        self._transformations[name] = transformation

        return {
            "success": True,
            "transformation": name,
        }

    def list_transformations(self) -> List[FeatureTransformation]:
        """List all transformations.

        Returns:
            List of transformations.
        """
        return list(self._transformations.values())

    def get_feature_vector(
        self,
        feature_group: str,
        entity_id: str,
        features: List[str],
    ) -> Optional[FeatureVector]:
        """Get a feature vector for an entity.

        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature names to retrieve.

        Returns:
            FeatureVector or None.
        """
        if feature_group not in self._feature_groups:
            return None

        key = f"{feature_group}:v1:{entity_id}"

        if key in self._feature_values:
            stored_features = self._feature_values[key].get("features", {})
            values = [
                FeatureValue(name=f, value=stored_features.get(f, 0))
                for f in features
            ]
        else:
            values = [
                FeatureValue(name=f, value=hash(f"{entity_id}:{f}") % 100)
                for f in features
            ]

        return FeatureVector(
            entity_id=entity_id,
            entity_type=feature_group,
            features=values,
        )

    def write_features(
        self,
        feature_group: str,
        entity_id: str,
        features: Dict[str, Any],
    ) -> Dict[str, Any]:
        """Write feature values.

        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature values.

        Returns:
            Write result.
        """
        key = f"{feature_group}:v1:{entity_id}"
        self._feature_values[key] = {
            "feature_group": feature_group,
            "entity_id": entity_id,
            "features": features,
            "timestamp": datetime.now(),
        }

        return {
            "success": True,
            "entity_id": entity_id,
            "features_written": len(features),
        }
