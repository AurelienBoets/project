package org.acme.entity;


import org.bson.types.ObjectId;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@MongoEntity(collection = "category")
public class CategoryEntity extends ReactivePanacheMongoEntity {
    private String name;

    public void setId(ObjectId id) {
        this.id = id;
    }
}
