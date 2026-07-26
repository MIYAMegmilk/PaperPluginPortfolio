# ItemTraitsStats

[ItemTraits](../ItemTraits/) のイベントを購読し、トレイト値をMinecraftの `AttributeModifier` として実際のゲームプレイに反映する連携プラグイン。

## 動作環境

- Paper 1.21.1
- Java 21
- **ItemTraits** プラグイン

## 導入方法

1. ビルド: `./gradlew build`
2. `ItemTraitsStats-1.0.0.jar` を `ItemTraits-1.0.0.jar` と一緒に `plugins/` に配置
3. サーバー起動で `config.yml` が自動生成される
4. `config.yml` のバインディングをItemTraitsのトレイト定義に合わせて編集

## config.yml

```yaml
bindings:
  attack:                              # ItemTraitsのトレイトキーと一致させる
    attribute: GENERIC_ATTACK_DAMAGE   # BukkitのAttribute列挙名
    operation: ADD_NUMBER              # ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
    slot: HAND                         # HAND | OFF_HAND | HEAD | CHEST | LEGS | FEET | ANY
  crit_rate:
    attribute: GENERIC_ATTACK_DAMAGE
    operation: ADD_SCALAR
    slot: HAND
    scale: 0.01                        # トレイト値にこの係数を掛ける（デフォルト: 1.0）
  defense:
    attribute: GENERIC_ARMOR
    operation: ADD_NUMBER
    slot: ANY
```

| フィールド | 必須 | 説明 |
|-----------|------|------|
| `attribute` | はい | Bukkit `Attribute` 列挙定数 |
| `operation` | はい | 基本値への適用方法 |
| `slot` | いいえ | 装備スロットフィルタ（デフォルト: `ANY`） |
| `scale` | いいえ | 適用前の乗数（デフォルト: `1.0`）。パーセント系トレイトに使う |

## 仕組み

1. `TraitsGenerateEvent` と `TraitsTransformEvent` を `MONITOR` 優先度で購読
2. ItemTraitsがPDCとロアを書き込んだ次のtickで、確定したトレイト値を読み取る
3. `NamespacedKey` で識別して既存のモディファイアを削除
4. 新しい `AttributeModifier` を `ItemMeta` に追加

このプラグインはトレイト値を変更しない。読み取って属性モディファイアに変換するだけ。

## ライセンス

MIT
