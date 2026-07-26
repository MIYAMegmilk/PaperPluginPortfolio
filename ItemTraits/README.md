# ItemTraits

アイテム個別にランダムなステータス値（トレイト）を付与し、PersistentDataContainerで永続化するPaperプラグイン。他プラグインから利用可能なAPIを提供する。

## 動作環境

- Paper 1.21.1
- Java 21

## 導入方法

1. ビルド: `./gradlew build`
2. `build/libs/ItemTraits-1.0.0.jar` をサーバーの `plugins/` にコピー
3. サーバー起動で `config.yml` が自動生成される
4. `config.yml` でトレイト定義を編集し、サーバーを再起動

## config.yml

```yaml
schema-version: 1

traits:
  attack:
    display-name: "Attack Power"   # 表示名
    min: 1.0                       # 最小値
    max: 20.0                      # 最大値
    precision: 1                   # 小数点以下の桁数（0で整数）
    format: "+{value}"             # 表示フォーマット
  crit_rate:
    display-name: "Critical Rate"
    min: 0.0
    max: 100.0
    precision: 0
    format: "{value}%"
  defense:
    display-name: "Defense"
    min: 1.0
    max: 15.0
    precision: 0
    format: "+{value}"
```

| フィールド | 必須 | 説明 |
|-----------|------|------|
| `display-name` | いいえ | 表示名（未指定時はキー名を使用） |
| `min` | いいえ | 下限値（デフォルト: 0） |
| `max` | いいえ | 上限値（デフォルト: 10） |
| `precision` | いいえ | 小数桁数（デフォルト: 0） |
| `format` | いいえ | 表示テンプレート（デフォルト: `{value}`） |

## コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/traits inspect` | `itemtraits.inspect`（デフォルト: 全員） | 手持ちアイテムのトレイトをチャットに表示 |
| `/traits gui` | `itemtraits.inspect`（デフォルト: 全員） | インベントリ内のトレイト付きアイテムをGUIで一覧表示 |
| `/traits generate` | `itemtraits.generate`（デフォルト: OP） | 手持ちアイテムにランダムなトレイトを付与 |

## API

他プラグインからトレイトの読み書き・転送が可能。

### 依存関係（build.gradle.kts）

```kotlin
compileOnly(files("libs/ItemTraits-1.0.0.jar"))
```

### API取得

```java
import dev.itemtraits.api.ItemTraitsAPI;

RegisteredServiceProvider<ItemTraitsAPI> rsp =
    Bukkit.getServicesManager().getRegistration(ItemTraitsAPI.class);
if (rsp != null) {
    ItemTraitsAPI api = rsp.getProvider();
}
```

### トレイトの読み取り

```java
Optional<TraitHolder> traits = api.getTraits(itemStack);
traits.ifPresent(t -> {
    double atk = t.get("attack");    // 未定義なら0.0
    boolean hasCrit = t.has("crit_rate");
});
```

### ランダム生成

```java
// 定義済みの全トレイトをロールしてPDCに書き込む
// TraitsGenerateEvent（キャンセル可能）が発火する
api.generateTraits(itemStack);
```

### アイテム間のトレイト転送

```java
// sourceからtargetへ全トレイトをコピー
// TraitsTransformEvent（キャンセル可能、トレイト変更可能）が発火する
api.transformTraits(sourceItem, targetItem);
```

### 任意のトレイト書き込み

```java
import dev.itemtraits.api.TraitHolder;

TraitHolder custom = new TraitHolder(Map.of("attack", 15.0, "defense", 8.0));
api.applyTraits(itemStack, custom);
```

## イベント

| イベント | タイミング | キャンセル | 変更可能 |
|---------|-----------|----------|---------|
| `TraitsGenerateEvent` | ロール後、PDC書き込み前 | 可 | `setTraits(TraitHolder)` |
| `TraitsTransformEvent` | ソース読み取り後、ターゲット書き込み前 | 可 | `setTraits(TraitHolder)` |

### 使用例: 転送時にトレイト強化

```java
@EventHandler
public void onTransform(TraitsTransformEvent event) {
    if (isRareMaterial(event.getSource())) {
        TraitHolder boosted = event.getTraits().multiply("attack", 1.5);
        event.setTraits(boosted);
    }
}
```

## 連携プラグイン: ItemTraitsStats

[ItemTraitsStats](../ItemTraitsStats/) はこのAPIを利用し、トレイト値をMinecraftの`AttributeModifier`として反映するプラグイン。ServicesManagerとイベントを通じて連携しており、ItemTraits本体のコード変更は不要。

## PDCストレージ形式

トレイトは `itemtraits:data` キーに単一のJSON文字列として保存:

```json
{"v":1,"traits":{"attack":12.5,"crit_rate":45,"defense":7}}
```

`v` フィールドにより将来のスキーマ移行時にデータを失わずに済む。

## ライセンス

MIT
