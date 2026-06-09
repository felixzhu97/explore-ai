"""Repository interface for Feature Store operations."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Dict, Any, List

@dataclass(frozen=True)
class FeatureGroupInfo:
    """Value object for feature group metadata."""
    name: str
    version: str
    feature_count: int

class FeatureStoreRepository(ABC):
    """Abstract repository for Feature Store operations."""
    
    @abstractmethod
    def create_feature_group(
        self,
        name: str,
        version: str,
        schema: List[Dict[str, Any]]
    ) -> FeatureGroupInfo:
        """Create a new feature group."""
        pass
    
    @abstractmethod
    def list_feature_groups(self) -> List[FeatureGroupInfo]:
        """List all feature groups."""
        pass
    
    @abstractmethod
    def get_feature_group(self, name: str, version: str) -> FeatureGroupInfo:
        """Get feature group details."""
        pass
    
    @abstractmethod
    def write_feature_group(
        self,
        name: str,
        version: str,
        features: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Write features to a feature group."""
        pass
    
    @abstractmethod
    def get_historical_features(
        self,
        name: str,
        version: str,
        entity_ids: List[str]
    ) -> List[Dict[str, Any]]:
        """Get historical features for entities."""
        pass
