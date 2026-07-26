# Paper Plugin Portfolio

Paper 1.21.1 向けのプラグイン4点セット。アイテムへのステータス付与、属性反映、モード切替武器、GUI進化合成の機能を、プラグイン間連携を意識した設計で実装しています。

## 動作環境

- Paper 1.21.1
- Java 21

## すぐに試す

`plugins/` フォルダにビルド済みのjarが入っています。4つ全てをサーバーの `plugins/` にコピーして起動するだけで動作します。

```
plugins/
├── ItemTraits-1.0.0.jar
├── ItemTraitsStats-1.0.0.jar
├── WeaponModes-1.0.0.jar
└── ItemForge-1.0.0.jar
```

## プラグイン一覧

### 1. ItemTraits — アイテム個別トレイトシステム

アイテムごとにランダムなステータス値（トレイト）を生成し、PersistentDataContainer（PDC）にJSON形式で永続化する。他プラグイン向けのAPIをBukkitの `ServicesManager` で公開しており、トレイトの読み書き・転送が可能。

#### コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/traits inspect` | `itemtraits.inspect`（全員） | 手持ちアイテムのトレイトをチャットに表示 |
| `/traits gui` | `itemtraits.inspect`（全員） | インベントリ内のトレイト付きアイテムをGUIで一覧表示 |
| `/traits generate` | `itemtraits.generate`（OP） | 手持ちアイテムにランダムなトレイトを付与 |

#### 主な技術要素

- **PDC + JSON永続化**: `{"v":1,"traits":{...}}` 形式でバージョン付きスキーマ管理
- **ServicesManager API公開**: `ItemTraitsAPI` インターフェースを登録。他プラグインは `compileOnly` 依存で利用可能
- **カスタムイベント**: `TraitsGenerateEvent` / `TraitsTransformEvent`（両方キャンセル可能、トレイト変更可能）
- **チェストGUI**: インベントリ内アイテムの一覧表示、クリックで詳細ビュー（バー表示 + アイコンティア判定）
- **config駆動**: トレイト定義（名前・範囲・精度・表示フォーマット）を全てYAMLで管理
- **ユニットテスト**: TraitSerializer / TraitRoller / TraitRegistry に対する計16テスト

#### config.yml

```yaml
schema-version: 1
traits:
  attack:
    display-name: "Attack Power"
    min: 1.0
    max: 20.0
    precision: 1
    format: "+{value}"
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

#### API使用例

```java
// API取得
RegisteredServiceProvider<ItemTraitsAPI> rsp =
    Bukkit.getServicesManager().getRegistration(ItemTraitsAPI.class);
ItemTraitsAPI api = rsp.getProvider();

// トレイト読み取り
Optional<TraitHolder> traits = api.getTraits(itemStack);

// ランダム生成（TraitsGenerateEventが発火）
api.generateTraits(itemStack);

// アイテム間転送（TraitsTransformEventが発火）
api.transformTraits(sourceItem, targetItem);

// 任意の値を書き込み
api.applyTraits(itemStack, new TraitHolder(Map.of("attack", 15.0)));
```

#### PDCストレージ形式

```json
{"v":1,"traits":{"attack":12.5,"crit_rate":45,"defense":7}}
```

`v` フィールドにより将来のスキーマ移行時にデータを失わずに済む。

---

### 2. ItemTraitsStats — トレイト→属性モディファイア変換

ItemTraitsのイベントを `MONITOR` 優先度で購読し、トレイト値をMinecraftの `AttributeModifier` としてゲームプレイに反映する連携プラグイン。

#### 主な技術要素

- **イベント駆動連携**: ItemTraitsのコード変更なしで動作。`compileOnly` 依存 + `ServicesManager`
- **次tick適用**: イベント発火時はPDC未書き込みのため、`BukkitScheduler` で次tickに遅延実行
- **NamespacedKey管理**: モディファイア追加前に同じキーの既存モディファイアを削除して重複防止

#### config.yml

```yaml
bindings:
  attack:
    attribute: GENERIC_ATTACK_DAMAGE
    operation: ADD_NUMBER
    slot: HAND
  crit_rate:
    attribute: GENERIC_ATTACK_DAMAGE
    operation: ADD_SCALAR
    slot: HAND
    scale: 0.01          # トレイト値にこの係数を掛ける
  defense:
    attribute: GENERIC_ARMOR
    operation: ADD_NUMBER
    slot: ANY
```

---

### 3. WeaponModes — モード切替武器システム

config.ymlで武器と複数のモードを定義し、右クリックでモードを切り替える。各モードに属性モディファイア・ライフスティール・ヒット時デバフ・装備中バフを設定可能。

#### コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/wm give <武器名>` | `weaponmodes.admin`（OP） | 武器をプレイヤーに付与 |
| `/wm list` | `weaponmodes.admin`（OP） | 登録済み武器の一覧表示 |

#### 主な技術要素

- **PDCモード管理**: モードインデックスを `weaponmodes:mode` キーで保存。名前変更に強い
- **カスタムイベント**: `WeaponModeSwitchEvent`（キャンセル可能）
- **ライフスティール**: `EntityDamageByEntityEvent` で最終ダメージの指定割合を回復
- **ヒット時ポーション**: 攻撃対象にポーション効果を付与
- **装備中効果**: `PlayerItemHeldEvent` でプレイヤーごとにタスクを起動/停止（グローバルポーリングなし）
- **タブ補完**: 武器名の補完対応

#### config.yml（抜粋）

```yaml
weapons:
  phantom_blade:
    display-name: "Phantom Blade"
    base-material: NETHERITE_SWORD
    modes:
      normal:
        display-name: "&fPhantom Blade &8[&7Normal&8]"
        lore:
          - "&7A balanced stance."
        attributes:
          attack_damage:
            attribute: GENERIC_ATTACK_DAMAGE
            value: 8.0
            operation: ADD_NUMBER
            slot: MAINHAND

      offensive:
        display-name: "&cPhantom Blade &8[&4Offensive&8]"
        attributes:
          attack_damage:
            attribute: GENERIC_ATTACK_DAMAGE
            value: 14.0
            operation: ADD_NUMBER
            slot: MAINHAND
        on-hit:
          lifesteal: 0.20                 # 与ダメージの20%回復
          effects:
            - "SLOWNESS:1:60"             # TYPE:LEVEL:DURATION_TICKS

      defensive:
        display-name: "&9Phantom Blade &8[&9Defensive&8]"
        attributes:
          generic_armor:
            attribute: GENERIC_ARMOR
            value: 10.0
            operation: ADD_NUMBER
            slot: MAINHAND
        while-held:
          effects:
            - "RESISTANCE:1"             # 持っている間耐性付与
```

---

### 4. ItemForge — GUI式アイテム進化合成

武器と素材をチェストGUIに配置し、上位アイテムへ進化させるシステム。進化時にItemTraits APIを通じてトレイトが引き継がれる。

#### コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/forge` | `itemforge.use`（全員） | 進化合成GUIを開く |

#### 主な技術要素

- **チェストGUI**: 5行GUI。入力スロット・素材スロット（最大3種）・出力プレビュー・確認ボタン
- **リアルタイムプレビュー**: アイテム配置/除去のたびに次tickでレシピ判定とプレビュー更新
- **トレイト引き継ぎ**: `ItemTraitsAPI.transformTraits()` で進化元のトレイトを進化先に転送
- **アイテム安全性**: GUI終了時に入力・素材スロットのアイテムをプレイヤーに返却。インベントリ満杯時は足元にドロップ
- **連鎖進化**: 鉄剣→ダイヤ剣→ネザライト剣のように段階的な進化が可能

#### GUIレイアウト

```
行0:  ■ ■ ■ ■ ■ ■ ■ ■ ■
行1:  ■ [入力] ■ [状態] ■ [出力] ■ ■ ■
行2:  ■ ■ ■ ■ ■ ■ ■ ■ ■
行3:  ■ [素材1] [素材2] [素材3] ■ [確認] ■ ■ ■
行4:  ■ ■ ■ ■ ■ ■ ■ ■ ■
```

#### config.yml

```yaml
recipes:
  iron_to_diamond:
    input: IRON_SWORD
    output:
      material: DIAMOND_SWORD
      name: "&bEvolved Diamond Blade"
      lore:
        - "&7Forged from iron through evolution."
    materials:
      - "DIAMOND:3"
      - "BLAZE_POWDER:2"

  diamond_to_netherite:
    input: DIAMOND_SWORD
    output:
      material: NETHERITE_SWORD
      name: "&6Ascended Netherite Blade"
      lore:
        - "&7Reforged in the flames of the Nether."
    materials:
      - "NETHERITE_INGOT:2"
      - "NETHER_STAR:1"

  bow_upgrade:
    input: BOW
    output:
      material: CROSSBOW
      name: "&dPhantom Crossbow"
      lore:
        - "&7An ancient bow, reborn."
    materials:
      - "PHANTOM_MEMBRANE:4"
      - "END_ROD:1"
```

---

## プラグイン間の依存関係

```
ItemTraits（API提供）
├── ItemTraitsStats（イベント購読 → AttributeModifier適用）
└── ItemForge（API呼び出し → トレイト転送）

WeaponModes（独立動作）
```

- ItemTraits が API を `ServicesManager` で公開
- ItemTraitsStats と ItemForge は `compileOnly` でItemTraitsに依存（実行時のみ必要）
- WeaponModes は完全に独立して動作

## ビルド方法

各プラグインディレクトリで個別にビルド:

```bash
cd ItemTraits && ./gradlew build
cd ItemTraitsStats && ./gradlew build
cd WeaponModes && ./gradlew build
cd ItemForge && ./gradlew build
```

※ ItemTraitsStats と ItemForge のビルドにはItemTraitsのjarが `libs/` に必要。

## ライセンス

MIT
